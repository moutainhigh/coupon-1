package com.common.util.jd;

import com.alibaba.fastjson.JSONObject;
import com.common.constant.Constants;
import com.common.util.HttpUtils;
import com.google.common.collect.Lists;
import com.xiaoleilu.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zf
 * since 2019/12/20
 */
@Slf4j
public class Utils {
  /**
   * 判断当前时间是否在某个时间区间内
   *
   * @param startTime
   * @param endTime
   * @return
   */
  public static boolean dateInSection(Long startTime, Long endTime) {
    DateTime now = DateTime.now();
    if (now.isAfter(startTime) && now.isBefore(endTime)) {
      return true;
    }
    return false;
  }

  /**
   * 获取商品优惠券二合一连接
   *
   * @param str 商品skuId
   * @return
   */
  public static String getShortUrl(String str) {

    //蚂蚁星球地址
    String URL = Constants.ANT_SERVER_URL;

    HashMap map = new HashMap();
    map.put("apikey", Constants.ANT_APP_KEY);
    map.put("goods_id", str);

    map.put("positionid", Constants.JD_TGW_ID);
    //	type=1 goods_id=商品ID，type=2 goods_id=店铺id，type=3 goods_id=自定义链接(京东活动链接、二合一链接)
    map.put("type", "3");

    String requestResult = HttpUtils.post(URL, JSONUtil.toJsonPrettyStr(map));
    String twoToOneUrl = JSONObject.parseObject(requestResult.replace("\\", "")).getString("data");

    return twoToOneUrl;
  }

  /**
   * 递归遍历字符串中所有需要转链的链接
   *
   * @param content
   * @return
   */
  public static Map<String, String> getUrlMap2(String allcontent, String content, Map<String, String> map, int flag) {


    int i1 = content.indexOf("https://u.jd.com/");

    if (i1 != -1) {
      int start = i1;
      int end = i1 + 23;
      String substring = content.substring(start, end);

      String substring1 = content.substring(end);
      int i = allcontent.indexOf(substring);
      if (!substring.contains("http")) {
        return null;
      }
      String shortUrl = toLink_ddx(substring);

      if (StringUtils.isEmpty(shortUrl)) {
        return null;
      }
      map.put(substring, shortUrl);
      map.putAll(getUrlMap2(allcontent, substring1, map, i));
    }
    return map;
  }

  /**
   * 获取商品的图片地址
   *
   * @param skuId 商品skuId
   * @return
   */
  public static String getSKUInfo(String skuId) {
    //蚂蚁星球地址
    String URL = "http://api-gw.haojingke.com/index.php/v1/api/jd/goodsdetail";

    HashMap map = new HashMap();
    map.put("apikey", Constants.ANT_APP_KEY);
    map.put("goods_id", skuId);
    map.put("isunion", "0");

    String requestResult = HttpUtils.post(URL, JSONUtil.toJsonPrettyStr(map));

    return requestResult;
  }

  /**
   * 喵有券 根据淘宝商品淘口令转链转为自己的淘口令
   *
   * @return 转链结果内容
   */
  public static List<String> tbToLink(String tkl) {
    if (StringUtils.isEmpty(tkl)) {
      return null;
    }
    List<String> list = Lists.newArrayList();

    String format = String.format(Constants.TKL_TO_SKU_INFO_REQUEST_URL, Constants.MYB_APPKey, Constants.tb_name, Constants.TBLM_PID, tkl);
    String request = HttpUtils.getRequest(format);
    String substring = request.substring(0, request.lastIndexOf("}") + 1);
    log.info("taobao转链后的字符串------------>:{}", substring);

    if (200 == Integer.parseInt(JSONObject.parseObject(substring).getString("code"))) {
      String string = JSONObject.parseObject(substring).getJSONObject("data").getString("tpwd");
      if (StringUtils.isEmpty(string)) {
        return null;
      }

      String short_url = yunHomeToshortLink(Constants.TB_COPY_PAGE + string.replaceAll("￥", ""));
      if (StringUtils.isEmpty(short_url)) {
        log.info("长链接转短链接失败了----------------------->");
        return null;
      }

      list.add(short_url);
      String img_url = JSONObject.parseObject(substring).getJSONObject("data").getJSONObject("item_info").getString("pict_url");
      list.add(img_url);
      return list;
    } else {
      return null;
    }
  }


  /**
   * 将原字符串中的所有连接替换为转链之后的连接 ，返回新的字符串 (订单侠)
   *
   * @param str
   * @return
   */
  public static List<String> toLinkByDDX(String str, String reminder, String taobaoRobotId) {
    List<String> list = Lists.newArrayList();
    //淘宝转链
    if (!StringUtils.isEmpty(taobaoRobotId)) {
      String substring = "";
      String pattern = "([\\p{Sc}|(])\\w{8,12}([\\p{Sc}|)])";
      Pattern r = Pattern.compile(pattern);
      Matcher m = r.matcher(str);
      if (m.find()) {
        substring = m.group();
      } else {
        return null;
      }
      list = tbToLink(substring);
      if (Objects.isNull(list)) {
        return null;
      }
      String replace;
      if (str.contains("http://t.uc.cn")) {
        replace = str.replace(substring, list.get(0)).replace(str.substring(str.indexOf("http"), str.indexOf("http") + 22), "");

      } else {
        replace = str.replace(substring, list.get(0));
      }

      if (!replace.contains("【淘宝") && !replace.contains("[淘宝")) {
        replace = "【淘宝】" + replace;
      }
      try {
        list.set(0, URLEncoder.encode(Utf8Util.remove4BytesUTF8Char(replace), "UTF-8"));
        return list;
      } catch (UnsupportedEncodingException e) {
        return null;
      }
    }
    try {
      //京东转链
      Map<String, String> urlMap = new HashMap<>();
      Map<String, String> map = getUrlMap2(str, str, urlMap, 0);
      if (Objects.equals(map, null) || map.size() == 0) {
        return null;
      }
      String str2 = str;
      for (Map.Entry<String, String> entry : map.entrySet()) {
        str2 = str2.replace(entry.getKey(), entry.getValue());
      }

      if (!str2.contains("【京东") && !str2.contains("[京东")) {
        str2 = "【京东】" + str2;
      }

      list.add(URLEncoder.encode(Utf8Util.remove4BytesUTF8Char(str2 + reminder), "UTF-8"));
      //京东商品的购买链接
      String sku_url = MapUtil.getFirstNotNull(map);
      String skuId = getSkuIdByUrl(sku_url);
      String sku_img_url = getImgUrlBySkuId(skuId);

      list.add(sku_img_url);
      return list;
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    return null;
  }


  /**
   * 根据订单侠对京东链接转链
   *
   * @param link
   * @return
   */
  public static String toLink_ddx(String link) {
    try {
      String str = Constants.DDX_TOLINK_URL;
      String format = String.format(str, Constants.DDX_APIKEY, link, Constants.JDLM_ID);
      String request = HttpUtils.getRequest(format);
      String substring = request.substring(0, request.lastIndexOf("}") + 1);
      if (200 == Integer.parseInt(JSONObject.parseObject(substring).getString("code"))) {
        return JSONObject.parseObject(substring).getJSONObject("data").getString("shortURL");
      } else {
        return null;
      }
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * 订单侠根据商品链接获取商品skuId
   *
   * @return
   */
  public static String getSkuIdByUrl(String url) {
    try {
      String str = Constants.DDX_GET_SKUID;
      String format = String.format(str, Constants.DDX_APIKEY, url);
      String request = HttpUtils.getRequest(format);
      String substring = request.substring(0, request.lastIndexOf("}") + 1);

      if (200 == Integer.parseInt(JSONObject.parseObject(substring).getString("code"))) {
        String string = JSONObject.parseObject(substring).getString("data");
        return string;
      } else {
        return null;
      }
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * 订单侠根据根据京东skuId获取商品图片的url
   *
   * @param skuId 京东商品skuId
   * @return
   */
  public static String getImgUrlBySkuId(String skuId) {
    if (StringUtils.isEmpty(skuId)) {
      return null;
    }
    try {
      String str = Constants.DDX_SKU_INFO;
      String format = String.format(str, Constants.DDX_APIKEY, skuId);
      String request = HttpUtils.getRequest(format);
      String substring = request.substring(0, request.lastIndexOf("}") + 1);

      if (200 == Integer.parseInt(JSONObject.parseObject(substring).getString("code"))) {
        String string = JSONObject.parseObject(substring).getJSONArray("data").getJSONObject(0).getString("imgUrl");
        return string;
      } else {
        return null;
      }
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * 26云后台 将长链接转短链接
   *
   * @param to_link
   * @return
   */
  public static String yunHomeToshortLink(String to_link) {

    try {
      //26云后台获取token的url
      String token_url = "http://26yun.hcetq.cn/api/token/getAccessToken?userName=zduomi2020&password=abc369369";
      String request1 = HttpUtils.getRequest(token_url).replaceAll("/n", "");
      if (0 != Integer.parseInt(JSONObject.parseObject(request1).getString("code"))) {
        return null;
      }
      String token = JSONObject.parseObject(request1).getJSONObject("data").getString("accessToken");
      //26云后台获取短链接的url
      String str = "http://26yun.hcetq.cn/api/shorturl/createShortUrl?accessToken=" + token + "&url=" + to_link + "&mode=shortUrl&shortType=1";
      String request = HttpUtils.getRequest(str).replaceAll("/n", "");
      if (0 != Integer.parseInt(JSONObject.parseObject(request1).getString("code"))) {
        return null;
      }
      String string = JSONObject.parseObject(request).getJSONObject("data").getString("shortUrl");
      return string;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  public static void main(String[] args) {
    String str = "0点 前100半价+卷=34.5元\n" +
        "回力 女鞋帆布鞋\n" +
        "  (fcAz1VPopqc)";
//String str="给本号私聊发送口令：1，即可加入京东撸货线报群\n" +
//    "给本号私聊发送口令：2，即可加入互助群\n" +
//    "给本号私聊发送口令：3，即可加入淘宝线报群\n" +
//    "给本号发送文字口令：买+商品关键词，如：买手机，即可推送淘宝相关的手机优惠信息，每发一次推一条信息，可反复发送\n" +
//    "给本号发送您在淘宝选中的商品链接或口令，有采集到隐藏卷亦会给您推送。\n" +
//    "本群宗旨以最小的成本最大利益化；线报群禁止发互助、广告等无关信息链接，互助可进互助群；；喜欢本群也可以推送给身边的亲友。\n" +
//    "qq群：748536791";
//    String pattern = "([(])\\w{8,12}([)])";

//    String pattern = "([\\p{Sc}|(])\\w{8,12}([\\p{Sc}|)])";
//
//      Pattern r = Pattern.compile(pattern);
//      Matcher m = r.matcher(str);
//      if (m.find()) {
//        System.out.println("match: " + m.group());
//      }


    String pattern = "([\\p{Sc}|(])\\w{8,12}([\\p{Sc}|)])";
    Pattern r = Pattern.compile(pattern);
    Matcher m = r.matcher(str);
    if (m.find()) {
      String substring = m.group();
      System.out.println(substring);

    }

  }
}
