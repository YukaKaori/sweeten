package com.itheima.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PointsLog {
    private Integer id;
    private Integer memberId;

    /** 类型 1=消费获取 2=生日赠送 3=活动赠送 4=积分兑换扣除 5=过期扣除 6=管理员调整 */
    private Integer type;

    /** 变动积分（正=增加，负=扣除） */
    private Integer points;

    /** 变动后余额 */
    private Integer balance;

    private String reason;
    private Integer relatedId;
    private LocalDateTime createdAt;
}
