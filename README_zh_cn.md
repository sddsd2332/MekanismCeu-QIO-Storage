# Mekanism CEU QIO Storage

适用于 Minecraft 1.12.2 和 Mekanism CE Unofficial 的 QIO 存储整合附属。
本项目为多个魔法、能源和科技模组提供专用 QIO 驱动器及传输适配器。

## 环境要求

- Minecraft 1.12.2
- Forge 14.23.5.2768 或更高版本
- Mekanism CE Unofficial

以下整合模组均为可选依赖。只有 Forge 检测到对应模组 ID 已加载时，才会
注册相应的 QIO 驱动器和适配器：

| 模组 | 模组 ID | QIO 资源 |
| --- | --- | --- |
| Thaumcraft | `thaumcraft` | 源质，每个 Aspect tag 对应一种 QIO 资源 |
| Botania | `botania` | 魔力 |
| Blood Magic | `bloodmagic` | 灵魂网络 LP；五种意志 |
| Astral Sorcery | `astralsorcery` | 星能 |
| Embers | `embers` | 灰烬能量 |
| Nature's Aura | `naturesaura` | 灵气 |
| PneumaticCraft: Repressurized | `pneumaticcraft` | 压缩空气 |

本附属使用 Mekanism 公开的可扩展 QIO API，不修改 Mekanism 源码，不通过反射
访问可选模组 API，也不要求安装 JEI 才能进行资源存储。可选模组 jar 仅作为
开发时的编译依赖；运行时通过 Forge 的模组加载检查隔离未安装的整合。

## QIO 驱动器和单位

每种资源都有四个与 Mekanism 对应的专用驱动器等级：`base`、`hyper_dense`、
`time_dilating` 和 `supermassive`。单一资源整合使用单类型驱动器。Blood Magic
意志驱动器的类型容量由五种意志共享。需要存储整数单位的整合会把原生资源的
小数余量保留在源设备中。

各资源驱动器显示的单位如下：

- Thaumcraft：`1` 单位 = `1` 点源质；每个 Aspect 都是独立资源。
- Botania：`1` 单位 = `1` 点魔力。
- Blood Magic LP：`1` 单位 = `1` 点灵魂网络 LP，并按所有者 UUID 区分网络。
- Blood Magic 意志：`1` 单位 = `1` 点意志，支持原生、腐蚀、破坏、复仇和坚毅
  五种类型，所有类型共享驱动器容量。
- Astral Sorcery：`1` 单位 = `1` 点祭坛星能；原生星辉网络的 `1` 点对应 QIO
  的 `200` 单位。星座只作为输出元数据，不会拆分成不同的存储类型。
- Embers：`1` 单位 = `1` 点灰烬能量；原生小数余量保留在源设备中。
- Nature's Aura：`1` 单位 = `1` 点灵气。
- PneumaticCraft：`1` 单位 = `1` 点空气；压力由原生处理器按“空气量 / 容积”计算。

## 整合行为

### Thaumcraft 源质

通过 Thaumcraft 的 `IAspectSource`、`IAspectContainer` 和 `IEssentiaTransport`
API 传输。相邻且兼容的传输面、容器和机器均可进行输入或输出。Aspect tag 会
作为稳定的 QIO 资源标识保存，不同 Aspect 不会共用同一个驱动器条目。

### Botania 魔力

通过 Botania 的魔力池和兼容接收器 API 传输，遵守魔力池容量、创造模式魔力池、
方向能力检查及模拟操作。QIO 资源选择可以识别带有魔力的物品和已存储魔力的驱动器。

### Blood Magic LP 与意志

LP 按 Blood Magic 灵魂网络所有者 UUID 存储。LP 传输使用原生灵魂网络的权限和
模拟机制，确保不同所有者的网络相互独立。

意志传输使用原生 `IDemonWillConduit` 和 `IDemonWillGem` API，支持相邻的恶魔
坩埚、恶魔石柱和恶魔结晶器，也支持意志物品、结晶、塔尔塔洛斯宝石以及已存储
意志的 QIO 驱动器。五种原生意志均可选择，并共享意志驱动器的容量。

### Astral Sorcery 星能

星能按一种通用资源存储，不区分星座。QIO 输入端可以接收原生星辉网络的星能，
输出端则通过普通输出面向相邻的 `IStarlightReceiver` 发送。祭坛和仪式台均可
接收输出；祭坛聚焦水晶和仪式台水晶只提供星座元数据，不会生成独立的 QIO
资源类型。无需绑定 Linking Tool，也无需注册 Astral Sorcery 光源。

### Embers 灰烬能量

通过目标面上的 Embers `IEmberCapability` 传输，遵守原生容量和模拟操作。QIO
存储整数灰烬能量，小数余量保留在提供方。QIO GUI 使用 Embers 原生
`particle_star` 粒子风格显示。

### Nature's Aura 灵气

通过有方向的 `IAuraContainer` 能力传输。输入前会检查世界灵气类型及容器是否
接受该类型，并遵守原生容量和模拟操作规则。

### PneumaticCraft 压缩空气

通过 `IPneumaticMachine` 获取 PneumaticCraft 的 `IAirHandler` 进行传输。QIO
保存处理器的非负整数空气量，压力仍由原生处理器根据空气量和容积计算。容量由
处理器的最大压力和容积计算，输入和输出会根据传输后的实际数值修正结果。QIO
GUI 使用 PneumaticCraft 原生空气粒子纹理显示。

## QIO GUI 显示

客户端资源渲染器与传输适配器使用相同的可选模组加载检查。GUI 会使用各模组
熟悉的原生视觉资源：Thaumcraft 的 Aspect 图标、Botania 的魔力水纹理、Blood
Magic 的 LP 和意志物品渲染、Astral Sorcery 的
`astralsorcery:itemshiftingstar`、Embers 的
`embers:entity/particle_star` 粒子、Nature's Aura 的灵气缓存纹理，以及
PneumaticCraft 的动态 `air_particle.png` 空气粒子。

## 开发

权威的 Mekanism API 依赖为：
`libs/Mekanism-CE-Unofficial-All-10.0.4.250-dev.jar`

```powershell
.\gradlew.bat compileJava --no-daemon
.\gradlew.bat test --no-daemon
```

可选整合的 smoke 检查会在 `run` 下创建专用世界，自动停止服务端或客户端，
并将报告和日志写入 `build/reports/qio-smoke`：

```powershell
.\gradlew.bat runServer --no-daemon -Pqio_smoke_test=true
.\gradlew.bat runServer --no-daemon -Pqio_smoke_test=true -Pqio_smoke_reload=true
.\gradlew.bat runServer --no-daemon -Pqio_smoke_test=true -Pqio_smoke_integration=will
.\gradlew.bat runClient --no-daemon -Pqio_smoke_test=true
```

在开发任务后追加 `-Penable_optional_mod_runtime=false`，可以验证未加载可选
模组时的类加载路径。Smoke 检查类仅用于开发，不会打包进发布版本。

英文文档见 [README.md](README.md)。
