[![996.icu](https://img.shields.io/badge/link-996.icu-red.svg)](https://github.com/996icu/996.ICU)

# Uranus
Uranus（乌拉诺斯）是一组基于Java构建的Web站点开发组件集。目标是当需要构建Java Web项目时可以专注于业务逻辑，无需为一些基础特性分神。

# maven打包

cmd
> mvn clean package -Dmaven.test.skip=true

windows powershell
> $mvnArgs1 ="mvn clean package -Dmaven.test.skip=true".replace('-D','`-D')<br />Invoke-Expression $mvnArgs1