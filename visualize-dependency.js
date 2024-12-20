#!/usr/bin/env node

import fs from 'fs';
import chalk from 'chalk';

// 基础颜色列表
const BASE_COLORS = [
    'red', 'green', 'yellow', 'blue', 'magenta', 'cyan', 'white',
    'gray', 'redBright', 'greenBright', 'yellowBright',
    'blueBright', 'magentaBright', 'cyanBright', 'whiteBright'
];

const BASE_EMOJIS = [
    // 动物类
    '🐒', '🐱', '🦊', '🐶', '🐻', '🐼', '🐲', '🐠', '🐞', '🦋', '🐔', '🦄', '🐬', 
    '🐯', '🦁', '🐷', '🐸', '🐰', '🐻‍❄️', '🐨', '🦝', '🦫', '🦘', '🦙', '🦌', '🦓', 
    '🦍', '🐵', '🦦', '🦥', '🦧', '🐺', '🦄', '🐐', '🐑', '🦙', '🦒', '🐪', '🐫',

    // 海洋生物
    '🐳', '🐬', '🐟', '🐠', '🐡', '🦈', '🐙', '🦑', '🦐', '🦞', '🦀', '🐚', '🦭',

    // 昆虫与小动物
    '🐝', '🐞', '🦋', '🐌', '🐛', '🦟', '🦗', '🕷️', '🦂', '🦠', '🐜', '🦫',

    // 水果
    '🍏', '🍎', '🍐', '🍊', '🍋', '🍌', '🍉', '🍇', '🍓', '🫐', '🍒', '🍑', '🥭', 
    '🍍', '🥥', '🥝', '🍅', '🥗', '🥑', '🥒',

    // 食物
    '🍔', '🍟', '🍕', '🌭', '🥪', '🌮', '🌯', '🥨', '🥚', '🍳', '🥐', '🥞', '🧇', 
    '🍝', '🍣', '🍜', '🍲', '🍛', '🍱', '🥟', '🦞', '🍤', '🍙', '🍘', '🍢', '🥫',

    // 甜点与零食
    '🍰', '🎂', '🧁', '🍮', '🍭', '🍬', '🍫', '🍿', '🍩', '🍪', '🌰', '🥜', '🍯',

    // 饮料
    '☕', '🍺', '🍻', '🥂', '🍷', '🍸', '🍹', '🧃', '🧊', '🍶', '🥤', '🧋',

    // 医疗与科学
    '💊', '💉', '⚗️', '🔬', '🧬', '🩺', '🩹', '🩸', '🏥', '🧪', '🐭',

    // 魔法与神秘
    '🔮', '🧿', '✨', '🌟', '💫', '☄️', '🌈', '🪄', '🎱',

    // 工具与职业
    '🛡️', '⚔️', '🔧', '🔨', '🔩', '⚙️', '⏰', '⌛', '🧰', '🔬', '🎨', '✏️', '📐',

    // 自然与天文
    '🌏', '🌙', '⭐', '☄️', '🔥', '🌋', '🌈', '🌊', '🌴', '🌵', '🌲', '🌳', '🍄',
    '🌻', '🌷', '🌱', '🍃', '🍂', '🍁', '🌿', '☘️', '🍀',

    // 表情
    '😀', '😃', '😄', '😁', '😆', '😅', '🤣', '😂', '🙂', '🙃', '😉', '😊', '😇', 
    '🥰', '😍', '🤩', '😘', '😗', '😚', '😙', '😋', '😛', '😜', '🤪', '😝', '🤑'
];

let packageMappings = {};
let packageCount = 0;

function resetMappings() {
    packageMappings = {};
    packageCount = 0;
}

function getMappingForPackage(pkg) {
    if (!packageMappings[pkg]) {
        const color = BASE_COLORS[packageCount % BASE_COLORS.length];
        const index = packageCount % BASE_EMOJIS.length;
        const round = Math.floor(packageCount / BASE_EMOJIS.length);

        let emoji = BASE_EMOJIS[index];
        if (round > 0) {
            emoji += round;
        }

        packageMappings[pkg] = { color, emoji };
        packageCount++;
    }
    return packageMappings[pkg];
}

function extractPackageName(fullName) {
    const parts = fullName.split(':');
    if (parts.length >= 2) {
        return parts[0] + ':' + parts[1];
    }
    return fullName;
}

class TreeNode {
    constructor(name, depth) {
        this.name = name;
        this.depth = depth;
        this.children = [];
        this.numbering = '';
    }
}

function assignNumbers(node, prefix = '') {
    node.children.forEach((child, index) => {
        const numbering = prefix === '' ? `${index + 1}` : `${prefix}.${index + 1}`;
        child.numbering = numbering;
        assignNumbers(child, numbering);
    });
}

function printTree(node, showRoot = false) {
    if (node.depth !== -1 || showRoot) {
        const pkgName = extractPackageName(node.name);
        const { color, emoji } = getMappingForPackage(pkgName);
        const colorFn = chalk[color] || (t => t);
        const prefix = node.numbering ? node.numbering + ' ' : '';
        console.log(prefix + colorFn(emoji + ' ' + node.name));
    }
    node.children.forEach(child => printTree(child, true));
}

function isNonDependencyLine(line) {
    return (
        line.startsWith('BUILD SUCCESS') ||
        line.startsWith('Total time') ||
        line.startsWith('Finished at') ||
        line.startsWith('Finished') ||
        line.startsWith('Scanning for projects...') ||
        line === ''
    );
}

function parseDepthAndName(line) {
    let trimmed = line.trimStart();

    let depthCount = 0;
    while (trimmed.startsWith("|  ")) {
        depthCount++;
        trimmed = trimmed.slice(3);
    }

    let depth = depthCount;
    if (trimmed.startsWith("+- ") || trimmed.startsWith("\\- ")) {
        depth = depthCount + 1;
        trimmed = trimmed.slice(3);
    }

    const name = trimmed.trim();
    return { depth, name };
}

function buildTree(lines) {
    const root = new TreeNode('root', -1);
    const stack = [root];
    let foundDependency = false;

    for (const line of lines) {
        const infoIdx = line.indexOf('[INFO]');
        if (infoIdx === -1) continue;
        let content = line.slice(infoIdx + 6).trim();

        if (isNonDependencyLine(content)) {
            continue;
        }

        // 必须包含冒号 ':' 才认为是依赖坐标行
        if (!content.includes(':')) {
            continue;
        }

        const { depth, name } = parseDepthAndName(content);
        if (!name.includes(':')) {
            continue;
        }

        const node = new TreeNode(name, depth);

        while (stack.length > 0 && stack[stack.length - 1].depth >= depth) {
            stack.pop();
        }

        stack[stack.length - 1].children.push(node);
        stack.push(node);
        foundDependency = true;
    }

    return foundDependency ? root : null;
}

async function main() {
    if (process.argv.length < 3) {
        console.error('请指定输入文件，如：node visualize-dependency.js tree.txt');
        process.exit(1);
    }

    const filePath = process.argv[2];
    let input = '';
    try {
        input = fs.readFileSync(filePath, 'utf-8');
    } catch (err) {
        console.error('读取文件失败:', err.message);
        process.exit(1);
    }

    const lines = input.split('\n');

    // 使用一个正则精确定义项目开始行
    // 该行一般形如: [INFO] --- maven-dependency-plugin:2.8:tree (default-cli) @ artifactId ---
    const projectStartRegex = /maven-dependency-plugin:.*:tree \(default-cli\) @ ([^ ]+) ---/;

    const projectBlocks = [];
    const projectNames = [];
    let currentBlock = [];
    let currentProjectName = null;
    let insideProject = false;

    for (const line of lines) {
        const match = line.match(projectStartRegex);
        if (match) {
            // 碰到新的项目开始行
            // 将之前的块结束并保存
            if (insideProject && currentBlock.length > 0) {
                projectBlocks.push(currentBlock);
                projectNames.push(currentProjectName || 'Unknown Project');
                currentBlock = [];
            }

            insideProject = true;
            currentProjectName = match[1];
            currentBlock.push(line);
        } else {
            if (insideProject) {
                currentBlock.push(line);
            }
            // 如果还没进入任何项目块，当前行不处理（说明是其他无关输出）
        }
    }

    // 文件末尾可能还有最后一个项目块没有结束
    if (insideProject && currentBlock.length > 0) {
        projectBlocks.push(currentBlock);
        projectNames.push(currentProjectName || 'Unknown Project');
    }

    if (projectBlocks.length === 0) {
        console.error('未解析到任何项目的依赖信息，请检查文件内容。');
        process.exit(1);
    }

    for (let i = 0; i < projectBlocks.length; i++) {
        resetMappings();
        const projectLines = projectBlocks[i];
        const root = buildTree(projectLines);

        console.log(chalk.blue.bold(`\n项目名称: ${projectNames[i]}\n`));

        if (!root) {
            console.error(`第 ${i+1} 个项目未解析到依赖信息，请检查输入。`);
        } else {
            assignNumbers(root);
            printTree(root, false);
        }

        if (i < projectBlocks.length - 1) {
            const nextProjectName = projectNames[i+1] ? projectNames[i+1] : '未知项目';
            console.log(chalk.gray('\n---------- 下一个项目 ----------'));
            console.log(chalk.gray(`即将分析: ${nextProjectName}\n`));
        }
    }
}

main();
