package com.goaway.contexts.activity.domain;

/**
 * 勋章档位（稀有度，递增）：拉 < NPC < 人上人 < 顶级 < 夯。
 * order 用于晋级比较与排序；colorKey 供前端映射稀有度配色。
 */
public enum BadgeTier {
    LA("拉", 0, "gray"),
    NPC("NPC", 1, "mint"),
    REN("人上人", 2, "lav"),
    TOP("顶级", 3, "gold"),
    HANG("夯", 4, "sakura");

    private final String label;
    private final int order;
    private final String colorKey;

    BadgeTier(String label, int order, String colorKey) {
        this.label = label;
        this.order = order;
        this.colorKey = colorKey;
    }

    public String getLabel() { return label; }
    public int getOrder() { return order; }
    public String getColorKey() { return colorKey; }

    /** 按 order 升序的全部档位（拉→夯）。 */
    public static final BadgeTier[] ASCENDING = { LA, NPC, REN, TOP, HANG };
}
