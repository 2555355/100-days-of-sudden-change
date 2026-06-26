package com.zombieapocalypse.ai;

import net.minecraft.block.BlockState;
import java.util.List;

/**
 * 方块收集接口
 * 僵尸破坏方块后收集材料，搭方块时消耗材料
 * 支持多方块库存（最多3个），增强搭桥能力
 */
public interface IBlockCollector {
    /** 获取当前可用的方块材料（不消耗） */
    BlockState peekCollectedBlock();
    /** 取出并消耗一个方块材料 */
    BlockState consumeCollectedBlock();
    /** 存入一个方块材料 */
    void addCollectedBlock(BlockState state);
    /** 是否有可用方块 */
    boolean hasCollectedBlock();
    /** 获取库存数量 */
    int getBlockInventorySize();
    /** 清空库存 */
    void clearBlockInventory();
}