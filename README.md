# GenshinGacha

## 简介
- 一个可以在[mcl](https://github.com/iTXTech/mirai-console-loader)中运行的模拟原神抽卡的机器人插件
- 调用[GenshinPray api](https://github.com/GardenHamster/GenshinPray)进行模拟抽卡

## 部署
- 下载并安装配置[mcl](https://github.com/iTXTech/mirai-console-loader)
- 从[releases](https://github.com/GardenHamster/GenshinGacha/releases)处下载最新版本jar包，并放入mcl的plugins目录中
- 在mcl目录下config路径中，创建com.hamster.pray.genshin文件夹，在目录中添加并修改[config.yml](https://github.com/GardenHamster/GenshinGacha/blob/master/config/com.hamster.pray.genshin/config.yml)文件，或者先运行mcl后自动生成该文件后再手动修改

## 指令
### 角色祈愿
```
#角色[十连/单抽]        #蛋池编号为0
#角色[十连/单抽]1       #蛋池编号为0
#角色[十连/单抽]2       #蛋池编号为1
#角色[十连/单抽]3       #蛋池编号为2
#角色[十连/单抽]...     #以此类推，编号未配置时会返回蛋池未配置提示
```

### 武器/常驻祈愿
- #武器[十连/单抽]
- #常驻[十连/单抽]

### 武器定轨
- #定轨神乐之真意

### 其他指令
- #蛋池
- #祈愿详情
- #祈愿记录
- #欧气排行
