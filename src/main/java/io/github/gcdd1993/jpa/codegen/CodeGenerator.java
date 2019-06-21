package io.github.gcdd1993.jpa.codegen;

import io.github.gcdd1993.jpa.codegen.config.CodeGeneratorConfig;
import io.github.gcdd1993.jpa.codegen.config.ModuleConfig;
import io.github.gcdd1993.jpa.codegen.exception.JpaCodegenException;
import io.github.gcdd1993.jpa.codegen.metadata.DefaultEntityInfoParser;
import io.github.gcdd1993.jpa.codegen.metadata.EntityInfo;
import io.github.gcdd1993.jpa.codegen.metadata.IEntityParser;
import io.github.gcdd1993.jpa.codegen.render.DefaultRender;
import io.github.gcdd1993.jpa.codegen.render.IRender;
import io.github.gcdd1993.jpa.codegen.util.ReflectUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TODO
 *
 * @author gaochen
 * Created on 2019/6/18.
 */
@Slf4j
public class CodeGenerator {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private static final String SRC_PATH = "src/main/";

    private CodeGeneratorConfig config;

    private Properties properties = new Properties();

    private List<String> moduleList = new LinkedList<>();

    private IEntityParser entityParser;

    private IRender render;

    public CodeGenerator(String configLocation) {
        try {
            properties.load(new FileInputStream(new File(configLocation)));

            config = new CodeGeneratorConfig();
            String entityPackage = properties.getProperty("entity.package");
            if (entityPackage == null) {
                throw new RuntimeException("must give me the entity package");
            }

            config.setEntityPackage(entityPackage);

            // io.github.gcdd1993.entity -> entity flag is entity
            config.setEntityFlag(entityPackage.substring(entityPackage.lastIndexOf(".") + 1));

            config.setAuthor(properties.getProperty("author", System.getProperty("user.name")));
            config.setComments(properties.getProperty("comments", "code generated by jpa codegen"));
            config.setDate(DATE_TIME_FORMATTER.format(LocalDate.now()));

            config.setFtlPath(properties.getProperty("template.dir", SRC_PATH + "resources/template/"));
            config.setCover(Boolean.parseBoolean(properties.getProperty("cover", "false")));

            // custom other params
            Map<String, String> otherParams = new HashMap<>(256);
            for (Object key : properties.keySet()) {
                String keyStr = key.toString();
                if (keyStr.contains(".") &&
                        "custom".equals(keyStr.substring(0, keyStr.indexOf(".")))) {
                    otherParams.put(keyStr.substring(keyStr.indexOf(".") + 1).replace(".", "_"), properties.getProperty(keyStr));
                }
            }
            config.setOtherParams(otherParams);

            // 实体解析器
            entityParser = new DefaultEntityInfoParser();

            // 渲染器
            render = new DefaultRender(config);

            log.info("init code generator success.");

        } catch (IOException e) {
            throw new JpaCodegenException("init code generator failed.", e);
        }
    }

    /**
     * 解析模块配置
     *
     * @param module 模块
     * @return 模块配置
     */
    private ModuleConfig parseModuleConfig(String module) {
        ModuleConfig moduleConfig = new ModuleConfig();
        moduleConfig.setClassNameSuffix(properties.getProperty(module + ".class.suffix",
                module.substring(0, 1).toUpperCase().concat(module.substring(1))));
        moduleConfig.setFtlName(config.getFtlPath() + properties.getProperty(module + ".ftlName", module + ".ftl"));

        String packageName = properties.getProperty(module + ".package");
        if (packageName != null) {
            moduleConfig.setPackageName(packageName);
            moduleConfig.setSavePath(SRC_PATH + "java/" + packageName.replace(".", "/") + "/");
        }

        return moduleConfig;
    }

    public void generate() {
        List<Class<?>> entityClasses = ReflectUtils.getClassListByAnnotation(config.getEntityPackage(), javax.persistence.Entity.class);
        List<EntityInfo> entityInfos = entityClasses.stream()
                .map(entityParser::parse)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!entityInfos.isEmpty()) {
            log.info("find {} entity classes, now start generate code.", entityInfos.size());

            entityInfos.forEach(entityInfo ->
                    moduleList.forEach(module -> render.render(entityInfo, module)));
        } else {
            log.warn("find none entity class, please check your entity package is true.");
        }
    }

    /**
     * 注册渲染组件
     *
     * @param module 模块名
     * @return 代码生成器本身
     */
    public CodeGenerator registerRender(String module) {
        ModuleConfig moduleConfig = parseModuleConfig(module);
        config.getModuleConfigMap().put(module, moduleConfig);
        moduleList.add(module);
        return this;
    }

}