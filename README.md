WeixinRouter
============

一个微信转发服务器。把微信公众平台高级开发接口的XML格式，转换为POST参数形式。另外数据返回统一使用JSON格式。
方便接口的开发和调试。

功能说明
=======
*  支持不同功能使用统一的入口， APP/USER/SESSION
*  支持接口统计功能
*  支持友好的错误提示，应用接口超时。会返回微信客户端信息。
*  支持多个应用接口集成。
*  支持多模块消息转发。 *开发中*


路由规则配置
==========
路由规则有两种方式可以配置。
*  在接口初始化时会发送一个， type=event, event=init_route_table 的消息。
返回JSON里面的，command字段就是路由配置规则。
*  在任意一个消息回复里面，都可以加入:command字段。更新路由表。

路由命令的格式：
```
 route -[AD] chain matchcondition [-poll] -j action appinfo [-expire time]
 
 chain => input|log
  
 matchcondition => -[type|event|key|ticket|content]
 action => forward|enter|log
 appinfo => -[app_type|app_url|app_token]
```

命令参数解释:
*  -[AD] -- 增加还是删除一个规则
*  chain -- 规则匹配的链表，目前有效的链表只有input。
*  matchcondition -- 消息匹配条件，支持type,event,key,content几个参数，具体值参考微信公众平台文档。
*  -poll -- 支持预先尝试发送消息到应用。如果返回不处理，尝试下一个应用。
*  action -- 转发的方式， forward-一次性转发， enter-切换式转发， log-只是把消息推送，不做响应检查。
*  appinfo -- 转发的应用信息
*  expire -- 规则过期时间，例如:10min

例子:
```bash
route -A input -type text -content liu -j forward -app_type xml -app_url http://wx2.emop.cn/route/51/3000052/1357 -app_token cb05694fd559dcfbacbac57ae2547733
route -A input -type text -content liu -j forward -app_type json -app_url http://emopselljd.sinaapp.com/api/wx_reply -app_token cb05694fd559dcfbacbac57ae2547733
```


