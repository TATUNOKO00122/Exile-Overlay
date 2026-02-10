package com.example.exile_overlay.client.damage;

import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Phase 3: 空間分割グリッドによるO(N)物理演算
 * 
 * 3D空間をセルに分割し、近接するオブジェクトのみをチェックすることで
 * O(N²)からO(N)への計算量削減を実現
 */
public class SpatialGrid {
    private static final float DEFAULT_CELL_SIZE = 2.0f; // デフォルトセルサイズ
    
    private final float cellSize;
    private final Map<Long, Set<DamageNumber>> grid = new HashMap<>();
    
    public SpatialGrid() {
        this(DEFAULT_CELL_SIZE);
    }
    
    public SpatialGrid(float cellSize) {
        this.cellSize = cellSize;
    }
    
    /**
     * グリッドをクリア
     */
    public void clear() {
        grid.clear();
    }
    
    /**
     * オブジェクトをグリッドに挿入
     */
    public void insert(DamageNumber number) {
        long key = calculateCellKey(number.getPosition());
        grid.computeIfAbsent(key, k -> new HashSet<>()).add(number);
    }
    
    /**
     * 指定されたオブジェクトの隣接セル内の全オブジェクトを取得
     * 最大27セル（3×3×3）をチェック
     */
    public Set<DamageNumber> getNeighbors(DamageNumber number) {
        Set<DamageNumber> neighbors = new HashSet<>();
        Vec3 pos = number.getPosition();
        
        // 現在のセルと隣接8セルを検索
        int centerX = (int) Math.floor(pos.x / cellSize);
        int centerY = (int) Math.floor(pos.y / cellSize);
        int centerZ = (int) Math.floor(pos.z / cellSize);
        
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    long key = calculateCellKey(centerX + dx, centerY + dy, centerZ + dz);
                    Set<DamageNumber> cell = grid.get(key);
                    if (cell != null) {
                        neighbors.addAll(cell);
                    }
                }
            }
        }
        
        neighbors.remove(number); // 自身を除外
        return neighbors;
    }
    
    /**
     * セルキーを計算（64ビットハッシュ）
     * 各座標は21ビットで表現（-1,048,576 ～ 1,048,575）
     */
    private long calculateCellKey(Vec3 pos) {
        int x = (int) Math.floor(pos.x / cellSize);
        int y = (int) Math.floor(pos.y / cellSize);
        int z = (int) Math.floor(pos.z / cellSize);
        return calculateCellKey(x, y, z);
    }
    
    private long calculateCellKey(int x, int y, int z) {
        // 各座標を21ビットに制限
        long lx = x & 0x1FFFFFL;
        long ly = y & 0x1FFFFFL;
        long lz = z & 0x1FFFFFL;
        
        // 64ビットキー生成
        return (lx << 42) | (ly << 21) | lz;
    }
    
    /**
     * グリッド内の総オブジェクト数を取得（デバッグ用）
     */
    public int getTotalObjectCount() {
        return grid.values().stream().mapToInt(Set::size).sum();
    }
    
    /**
     * グリッドのセル数を取得（デバッグ用）
     */
    public int getCellCount() {
        return grid.size();
    }
}
