package com.microtomcat.cluster.config;

import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ClusterConfigLoader {
    private static final String DEFAULT_CONFIG_PATH = "conf/cluster-config.xml";

    public ClusterConfig loadConfig() {
        return loadConfig(DEFAULT_CONFIG_PATH);
    }

    public ClusterConfig loadConfig(String configPath) {
        try {
            File configFile = new File(configPath);
            if (!configFile.exists()) {
                System.out.println("[ClusterConfigLoader] Config file not found: " + configPath);
                return createDefaultConfig();
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(configFile);
            doc.getDocumentElement().normalize();

            return parseDocument(doc);

        } catch (Exception e) {
            System.out.println("[ClusterConfigLoader] Error loading cluster configuration: " + e.getMessage());
            return createDefaultConfig();
        }
    }

    private ClusterConfig createDefaultConfig() {
        ClusterConfig config = new ClusterConfig();
        config.setClusterName("defaultCluster");
        
        ClusterConfig.NodeConfig defaultNode = new ClusterConfig.NodeConfig();
        defaultNode.setName("node1");
        defaultNode.setHost("localhost");
        defaultNode.setPort(8080);
        config.getNodes().add(defaultNode);
        
        System.out.println("[ClusterConfigLoader] Created default cluster configuration");
        return config;
    }

    private ClusterConfig parseDocument(Document doc) {
        ClusterConfig config = new ClusterConfig();
        
        // 解析基本配置
        Element root = doc.getDocumentElement();
        config.setClusterName(root.getAttribute("name"));
        
        // 解析心跳配置
        NodeList heartbeatIntervalNodes = root.getElementsByTagName("heartbeatInterval");
        if (heartbeatIntervalNodes.getLength() > 0) {
            config.setHeartbeatInterval(Long.parseLong(
                heartbeatIntervalNodes.item(0).getTextContent()));
        }
        
        NodeList heartbeatTimeoutNodes = root.getElementsByTagName("heartbeatTimeout");
        if (heartbeatTimeoutNodes.getLength() > 0) {
            config.setHeartbeatTimeout(Long.parseLong(
                heartbeatTimeoutNodes.item(0).getTextContent()));
        }

        // 解析节点配置
        NodeList nodeList = root.getElementsByTagName("node");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element nodeElement = (Element) nodeList.item(i);
            ClusterConfig.NodeConfig nodeConfig = new ClusterConfig.NodeConfig();
            nodeConfig.setName(nodeElement.getAttribute("name"));
            nodeConfig.setHost(nodeElement.getAttribute("host"));
            nodeConfig.setPort(Integer.parseInt(nodeElement.getAttribute("port")));
            config.getNodes().add(nodeConfig);
        }

        return config;
    }
} 