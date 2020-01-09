package com.jd.coupon.runner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @author zf
 * since 2020/1/2
 */
@Slf4j
@Component//被spring容器管理
@Order(3)//如果多个自定义ApplicationRunner，用来标明执行顺序
public class JdRunner implements ApplicationRunner {

  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  /**
   * 发送线报使用哪个群中的机器人
   */
  @Value("${message.robot.group}")
  private String robotGroup;

  @Override
  public void run(ApplicationArguments args) throws Exception {

//    List<String> robotIds = Lists.newArrayList();
//    robotIds.add("wxid_o7veppvw5bjn12");
//    robotIds.add("wxid_2r8n0q5v38h222");
//
//    robotIds.forEach(it -> {
//      //发送图片
//      WechatSendMsgDto wechatSendMsgDto = new WechatSendMsgDto(AllEnums.loveCatMsgType.FRIEND_MEMBER.getCode(), it, null, null, null, null, null);
//      wechatSendMsgDto.setIs_refresh("1");
//      String s1 = WechatUtils.sendWechatTextMsg(wechatSendMsgDto);
//
//      String str = JSONObject.parseObject(s1).getString("data");
//      String encode = URLUtil.decode(str, "UTF-8");
//
//      List<RobotFriend> robotFriends = JSONObject.parseArray(encode).toJavaList(RobotFriend.class);
//
//      robotFriends.forEach(item -> {
//        redisTemplate.opsForHash().put("wechat_friends", item.getWxid(), item.getNickname());
//      });
//
//    });
//    log.info("-----------------------------朋友加载完成-----------------------------");
  }


}