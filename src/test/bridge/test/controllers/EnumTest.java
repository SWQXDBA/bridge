package bridge.test.controllers;

import io.github.swqxdba.bridge.codegen.typescript.EnumConstantsGenerator;
import io.github.swqxdba.bridge.codegen.typescript.EnumConstantsGeneratorProvider;
import io.github.swqxdba.bridge.codegen.typescript.TsGenGlobalConfig;
import io.github.swqxdba.bridge.codegen.typescript.TsGenerator;
import io.github.swqxdba.bridge.meta.BridgeGlobalConfig;
import io.github.swqxdba.bridge.meta.BridgeUtil;
import io.github.swqxdba.bridge.meta.ControllerMeta;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EnumTest {

//    @Test
    void test(){

        //输入你项目的源码的绝对文件路径 该路径用于生成代码注释。如果不设置，生成的TS代码中就没有注释了。
        BridgeGlobalConfig.setSourceCodeDir(".");
        //指定扫描一个类所在包下的所有controller，可以指定一个条件来进行过滤
        List<ControllerMeta> controllerMetas = BridgeUtil.scan(getClass(),
                name -> name.contains("Controller"));

        //可以忽略一些controller的方法参数
        List<Class<?>> ignoreParamParentTypes = new ArrayList<>();
        ignoreParamParentTypes.add(ServletRequest.class);
        ignoreParamParentTypes.add(ServletResponse.class);
        TsGenGlobalConfig.getIgnoreParamParentTypes().addAll(ignoreParamParentTypes);
        TsGenGlobalConfig.setEnumConstantsGeneratorProvider(new EnumConstantsGeneratorProvider() {
            @NotNull
            @Override
            public List<EnumConstantsGenerator> getGenerators(@NotNull Class<? extends Enum<?>> clazz) {
                return Collections.singletonList(
                        EnumConstantsGenerator.createOptionGenerator(
                                Enum::name,
                                Enum::ordinal
                        )
                );
            }
        });

        TsGenerator tsGenerator = new TsGenerator();
        //生成时覆盖旧的文件
        tsGenerator.setOverrideFile(true);


        //设置生成的Ts路径,请确保生成的路径存在 可能需要手动创建一下文件夹
        tsGenerator.setBasePath("./test/api");

        //生成时 会把上述文件夹清空后重新生成
        tsGenerator.setCleanBasePath(true);
        //执行Ts代码生成
        tsGenerator.generate(controllerMetas);
    }
}
