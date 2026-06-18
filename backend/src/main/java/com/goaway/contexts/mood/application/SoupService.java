package com.goaway.contexts.mood.application;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 每日毒鸡汤：内置语料，零依赖、零成本。
 * daily() 按当天日期稳定返回（同一天同一条），random() 随机换一条。
 */
@Service
public class SoupService {

    private static final List<String> SOUPS = List.of(
            "钱难赚，屎难吃，但你还是得吃。",
            "今天不努力，明天还得努力，努力是逃不掉的。",
            "工资是公司怕你离职给的赔偿。",
            "别熬夜了，地球不爆炸，你的活也做不完。",
            "你不是一个人在战斗，你背后还有房贷。",
            "摸鱼一时爽，一直摸鱼一直爽。",
            "周一的你和周五的你，是两个物种。",
            "成年人的崩溃，从查看工资条开始。",
            "努力不一定成功，但不努力一定很轻松。",
            "你以为的休息，只是为了更好地加班。",
            "上班如上坟，下班如重生。",
            "老板画的饼，从来不分你一块。",
            "忙到最后你会发现，忙的是公司的事，瘦的是自己的命。",
            "钱包扁扁，梦想圆圆，可惜两者不能兑换。",
            "你品的不是咖啡，是续命的苦。"
    );

    public String daily(LocalDate date) {
        int idx = (int) Math.floorMod(date.toEpochDay(), SOUPS.size());
        return SOUPS.get(idx);
    }

    public String random() {
        return SOUPS.get(ThreadLocalRandom.current().nextInt(SOUPS.size()));
    }
}
