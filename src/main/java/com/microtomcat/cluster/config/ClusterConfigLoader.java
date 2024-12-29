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
                log("Config file not found: " + configPath);
                return createDefaultConfig();
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(configFile);
            doc.getDocumentElement().normalize();

            ClusterConfig config = new ClusterConfig();
            
            // 解析基本配置
            Element root = doc.getDocumentElement();
            config.setClusterName(root.getAttribute("name"));
            config.setHeartbeatInterval(Integer.parseInt(
                root.getAttribute("heartbeatInterval")));
            config.setHeartbeatTimeout(Integer.parseInt(
                root.getAttribute("heartbeatTimeout")));

            // 解析节点配置
            NodeList nodeList = doc.getElementsByTagName("node");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element nodeElement = (Element) nodeList.item(i);
                ClusterConfig.NodeConfig nodeConfig = new ClusterConfig.NodeConfig();
                nodeConfig.setName(nodeElement.getAttribute("name"));
                nodeConfig.setHost(nodeElement.getAttribute("host"));
                nodeConfig.setPort(Integer.parseInt(nodeElement.getAttribute("port")));
                config.getNodes().add(nodeConfig);
            }

            log("Successfully loaded cluster configuration");
            return config;

        } catch (Exception e) {
            log("Error loading cluster configuration: " + e.getMessage());
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
        
        log("Created default cluster configuration");
        return config;
    }

    private void log(String message) {
        System.out.println("[ClusterConfigLoader] " + message);
    }
} 