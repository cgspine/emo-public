<img src="image/emo.png" alt="logo" width="220" height="220"/> 

# emo - speed up android development

This repository contains series of libraries for android developers:

- ui-core: Contain some basic components such as TopBar, Loading, ... and some util methods.
- photo: To view/pick/clip pictures with Jetpack Compose.
- modal: A new way to implement Dialog，Toast, BottomSheet, ... with Jetpack Compose.
- permission: Request permission with tip.
- network: Get network state and Trace network traffic.
- js-bridge: js bridge for WebView.
- report: data report.
- config: use annotation and ksp to manage configs.
- scheme: a route library based on annotation and ksp.
- more libraries are on the way.

# Documentation

https://emo.qhplus.cn

# design principles

- Support API Level 24+.
- Based on JDK 11.
- All in Jetpack Compose.
- Use coroutines for concurrency and data flow.

# compose versions

<table>
 <th>
  <td>emo library version</td>
  <td>Compose Version</td>
  <td>Compose Compiler Version</td>
 </th>
 <tr>
  <td> 0.0.x </td>
  <td> 1.2.x </td>
  <td> - </td>
 </tr>
 <tr>
  <td> 0.1.x </td>
  <td> 1.3.0-beta03 </td>
  <td> 1.3.1 </td>
 </tr>
 <tr>
  <td> 0.2.x </td>
  <td> BOM 2022.10.00 </td>
  <td> 1.3.2 </td>
 </tr>
 <tr>
  <td> 0.3.x </td>
  <td> BOM 2022.11.00 </td>
  <td> 1.3.2 </td>
 </tr>
 <tr>
  <td> 0.4.x </td>
  <td> BOM 2023.01.00 </td>
  <td> 1.4.0 </td>
 </tr>
</table>

# Download

```kts
// core
implementation("cn.qhplus.emo:core:0.4.0")
// ui-core
implementation("cn.qhplus.emo:ui-core:0.4.0")
// photo
implementation("cn.qhplus.emo:photo-coil:0.4.0")
// modal
implementation("cn.qhplus.emo:modal:0.4.0")
// permission
implementation("cn.qhplus.emo:permission:0.4.0")
// network
implementation("cn.qhplus.emo:network:0.4.0")
// js-bridge
implementation("cn.qhplus.emo:js-bridge:0.4.0")
// config
implementation("cn.qhplus.emo:config-mmkv:0.4.0")
implementation("cn.qhplus.emo:config-panel:0.4.0")
ksp("cn.qhplus.emo:config-ksp:0.4.0")
// scheme
implementation("cn.qhplus.emo:scheme-impl:0.4.0")
ksp("cn.qhplus.emo:scheme-ksp:0.4.0")
// kv
implementation("cn.qhplus.emo:kv:0.4.0")
```

# Demo apk

[Download apk](https://emo.qhplus.cn/apks/emo.apk) or scan the qrcode below(pay attention to the wall):

![apk](image/apk-qr.png)

==========================================================================


关注我的公众号，获取 emo 相关的技术解析：

![公众号](image/subions.png)


给个赞赏，以资鼓励：

![赞赏码](image/reward.png)