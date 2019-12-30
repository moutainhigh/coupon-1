package com.jd.coupon.service;

import com.common.constant.AllEnums;
import com.common.constant.Constants;
import com.common.dto.robot.TLRobotRequestDto;
import com.common.dto.wechat.WechatReceiveMsgDto;
import com.common.dto.wechat.WechatSendMsgDto;
import com.common.util.robot.TLRobotUtils;
import com.common.util.robot.Utils;
import com.common.util.wechat.WechatUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author zf
 * since 2019/12/27
 */
@Service
@Slf4j
public class JdService {
  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  /**
   * 从love cat上接收微信消息
   *
   * @param receiveMsgDto
   */
  public void receiveWechatMsg(WechatReceiveMsgDto receiveMsgDto) {
    //记录机器人Id至缓存
    redisTemplate.opsForValue().setIfAbsent(AllEnums.wechatMemberFlag.ROBOT.getDesc(), receiveMsgDto.getRobot_wxid());
    //群事件
    if (Objects.equals(AllEnums.loveCatMsgType.GROUP_MSG.getCode(), receiveMsgDto.getType())) {
    //如果该群还没有被记录在缓存中，则添加缓存
      redisTemplate.opsForHash().putIfAbsent(AllEnums.wechatMemberFlag.GROUP.getDesc(), AllEnums.wechatGroupEnum.getStr(receiveMsgDto.getFrom_name()), receiveMsgDto.getFrom_wxid());
      //私聊事件
    } else if (Objects.equals(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), receiveMsgDto.getType())) {

    }

    log.info("接受到来自:{}的消息-->:{}", receiveMsgDto.getFrom_name(), receiveMsgDto.getMsg());

    //如果是@机器人则返@机器人的内容否则返回null(目前机器人只处理文字)
    String s = Utils.jqStr(receiveMsgDto.getMsg(), receiveMsgDto.getMsg_type());

    if (StringUtils.isNotBlank(s)) {

      TLRobotRequestDto robot = new TLRobotRequestDto();

      String result = TLRobotUtils.assembleRobotDto(robot, s);
      log.info("询问机器人问题:{},-------------------机器人回复内容:{}", s, result);
      WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.PRIVATE_MSG.getCode(), receiveMsgDto.getRobot_wxid(), receiveMsgDto.getFrom_wxid(), result, null);
      String s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
      log.info("微信返回:--->{}", s1);
    }
  }


  /**
   * 将消息模板存至缓存，等待机器人调用发送到群里
   *
   * @param content
   * @param imgName
   * @param groupName
   * @return
   */
  public boolean setMsgToRedis(String content, String imgName, String groupName) {
    String wechatGroupName = AllEnums.wechatGroupEnum.getStr(groupName);
    if (StringUtils.isEmpty(wechatGroupName)) {
      log.info("-----------------------没有找到群-----------------------");
      return false;
    }
    Long aLong;
    if (StringUtils.isEmpty(imgName)) {
      aLong = redisTemplate.opsForList().leftPush(wechatGroupName, content);
    } else {
      aLong = redisTemplate.opsForList().leftPush(wechatGroupName, content + Constants.SPLIT_FLAG + imgName);

    }
    if (aLong != 0L) {
      return true;
    }
    log.info("-----------------------将消息模板存至缓存失败-----------------------");
    return false;
  }
}