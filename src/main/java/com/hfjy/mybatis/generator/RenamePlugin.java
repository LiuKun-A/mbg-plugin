package com.hfjy.mybatis.generator;

import org.mybatis.generator.api.GeneratedJavaFile;
import org.mybatis.generator.api.GeneratedXmlFile;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.Document;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.codegen.XmlConstants;
import org.mybatis.generator.internal.util.StringUtility;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RenamePlugin extends PluginAdapter {
    private String searchStr;
    private String replaceStr;
    private Pattern pattern;
    private boolean replaceFlag;

    @Override
    public boolean validate(List<String> list) {
        searchStr = properties.getProperty("searchString");
        replaceStr = properties.getProperty("replaceString");
        boolean valid = StringUtility.stringHasValue(searchStr) && StringUtility.stringHasValue(replaceStr);

        if (valid) {
            pattern = Pattern.compile(searchStr);
            replaceFlag = true;
        } else {
            searchStr = "";
            replaceStr = "";
        }

        return true;
    }

    @Override
    public void initialized(IntrospectedTable introspectedTable) {
        //更改默认生成的Mapper.java为mbg目录下MBGMapper.java
        String oldType = introspectedTable.getMyBatis3JavaMapperType();
        if (replaceFlag) {
            Matcher matcher = pattern.matcher(oldType);
            oldType = matcher.replaceAll("MBG" + replaceStr);
        } else {
            oldType = oldType.replaceAll("Mapper", "MBGMapper");
        }
        int idx = oldType.lastIndexOf(".");
        if (idx > 0) {
            oldType = oldType.substring(0, idx) + ".mbg" + oldType.substring(idx);
        }
        introspectedTable.setMyBatis3JavaMapperType(oldType);
        //更改默认生成的Mapper.java为mbg目录下MBGMapper.java
        oldType = introspectedTable.getMyBatis3XmlMapperFileName();
        String mapperName = introspectedTable.getMyBatis3XmlMapperFileName();
        if (replaceFlag) {
            Matcher matcher = pattern.matcher(oldType);
            mapperName = matcher.replaceAll("MBG" + replaceStr);
        } else {
            mapperName = mapperName.replaceAll("Mapper", "MBGMapper");
        }
        introspectedTable.setMyBatis3XmlMapperFileName(mapperName);
        String mapperPkg = introspectedTable.getMyBatis3XmlMapperPackage() + File.separator + "mbg";
        introspectedTable.setMyBatis3XmlMapperPackage(mapperPkg);
    }

    @Override
    public List<GeneratedJavaFile> contextGenerateAdditionalJavaFiles(IntrospectedTable introspectedTable) {
        List<GeneratedJavaFile> result = new ArrayList<>();
        GeneratedJavaFile g = null;
        for (GeneratedJavaFile f : introspectedTable.getGeneratedJavaFiles()) {
            if (f.getFileName().contains("Dao") || f.getFileName().contains("Mapper")) {
                g = f;
                break;
            }
        }
        if (g != null) {
            String pkgName = g.getTargetPackage().replace("mbg", "custom");
            String className = g.getCompilationUnit().getType().getShortName().replace("MBG", "");
            Interface customInterface = new Interface(pkgName + "." + className);
            customInterface.setVisibility(JavaVisibility.PUBLIC);

            FullyQualifiedJavaType daoType = new FullyQualifiedJavaType(g.getCompilationUnit().getType().getFullyQualifiedName());
            customInterface.addSuperInterface(daoType);
            customInterface.addImportedType(daoType);
            String target = g.getTargetProject();
            String fileName = (target + File.separator + pkgName + "." + className).replace(".", File.separator);
            File file = new File(fileName + ".java");
            if (!file.exists()) {
                GeneratedJavaFile tmp = new GeneratedJavaFile(customInterface, target, context.getJavaFormatter());
                result.add(tmp);
            }
        }
        return result;
    }

    /**
     * 为相应的默认的xml文件生成一个自定义的xml，自己实现的可以都写在该文件中
     * @param introspectedTable
     * @return
     */
    @Override
    public List<GeneratedXmlFile> contextGenerateAdditionalXmlFiles(IntrospectedTable introspectedTable) {
        List<GeneratedXmlFile> result = new ArrayList<>();
        GeneratedXmlFile mbgXml = introspectedTable.getGeneratedXmlFiles().get(0);
        String projectName = mbgXml.getTargetProject();
        String packageName = mbgXml.getTargetPackage().replace("mbg", "custom");
        GeneratedJavaFile g = null;
        for (GeneratedJavaFile f : introspectedTable.getGeneratedJavaFiles()) {
            if (f.getFileName().contains("Dao") || f.getFileName().contains("Mapper")) {
                g = f;
                break;
            }
        }
        if (g != null) {
            Document document = new Document(XmlConstants.MYBATIS3_MAPPER_CONFIG_PUBLIC_ID,
                    XmlConstants.MYBATIS3_MAPPER_SYSTEM_ID);
            XmlElement root = new XmlElement("mapper");
            String className = g.getFileName().replace("MBG", "");
            String fileName = g.getFileName().replace("MBG", "").replace(".java", ".xml");
            String pkgName = g.getTargetPackage().replace(".mbg", ".custom");
            Attribute attribute = new Attribute("namespace", pkgName + "." + className.replace(".java", ""));
            root.addAttribute(attribute);
            document.setRootElement(root);

            File file = new File(projectName + File.separator + packageName + File.separator + fileName);
            if (!file.exists()) {
                GeneratedXmlFile gxf = new GeneratedXmlFile(document, fileName, packageName,
                        projectName, false, context.getXmlFormatter());
                result.add(gxf);
            }
        }
        return result;
    }
}
