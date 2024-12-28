import { promises as fs } from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

// 处理 __dirname 在 ES 模块中的替代方案
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// 指定要搜索的类名
const className = [
    "HttpServer",
    "Connector",
    "ConnectorMBean",
    "ProcessorPool",
    "ProcessorPoolMBean",
]

// 定义要搜索的根目录
const rootDir = path.resolve(__dirname);

// 定义输出文件的路径
const outputFilePath = path.join(rootDir, 'output.txt');

// 初始化或清空输出文件
await fs.writeFile(outputFilePath, '', 'utf8');

/**
 * 递归遍历目录并处理文件
 * @param {string} dir - 当前目录路径
 */
async function traverseDirectory(dir) {
    try {
        const filesAndDirs = await fs.readdir(dir, { withFileTypes: true });

        for (const item of filesAndDirs) {
            const fullPath = path.join(dir, item.name);

            if (item.isDirectory()) {
                // 忽略 node_modules 和其他你想忽略的目录
                if (item.name === 'node_modules' || item.name === '.git') {
                    continue;
                }
                // 递归遍历子目录
                await traverseDirectory(fullPath);
            } else if (item.isFile()) {
                const fileExtension = path.extname(item.name).toLowerCase();

                // 仅处理 .java 文件
                if (fileExtension !== '.java') {
                    continue;
                }

                // 检查文件名是否包含任何指定的类名
                const matches = className.some(name => item.name.includes(name));

                if (matches) {
                    try {
                        // 读取文件内容
                        const data = await fs.readFile(fullPath, 'utf8');
                        // 追加内容到输出文件，并添加文件路径作为分隔符
                        await fs.appendFile(outputFilePath, `\n\n=== ${fullPath} ===\n\n`, 'utf8');
                        await fs.appendFile(outputFilePath, data, 'utf8');
                        console.log(`已合并文件: ${fullPath}`);
                    } catch (err) {
                        console.error(`读取或写入文件时出错: ${fullPath}`, err);
                    }
                }
            }
        }
    } catch (err) {
        console.error(`遍历目录时出错: ${dir}`, err);
    }
}

// 开始遍历
await traverseDirectory(rootDir);

console.log(`所有匹配的 .java 文件已合并到 ${outputFilePath}`);
