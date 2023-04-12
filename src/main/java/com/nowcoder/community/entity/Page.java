package com.nowcoder.community.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 封装分页相关的信息.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Page {
    // 当前页码
    private int current = 1;
    // 显示上限
    private int limit = 10;
    // 数据总数(用于计算总页数)
    private int rows;
    // 查询路径(用于复用分页链(接)
    private String path;

    /**
     * 用户设置的当前页面必须>=1
     */
    public void setCurrent(int current) {
        if (current >= 1) this.current = current;
    }

    /**
     * 用户设置的每页记录数必须为[1,100]
     */
    public void setLimit(int limit) {
        if (limit >= 1 && limit <= 100) {
            this.limit = limit;
        }
    }

    public void setRows(int rows) {
        if (rows >= 0) this.rows = rows;
    }

    /**
     * 获取总页数
     *
     * @return
     */
    public int getTotal() {
        // rows / limit [+1]
        if (rows % limit == 0) { // 可以完全分页
            return rows / limit;
        } else {
            return rows / limit + 1;
        }
    }

    /**
     * 获取当前页的起始行,用于SQL查询
     *
     * @return
     */
    public int getOffset() {
        // current * limit - limit
        return (current - 1) * limit;
    }

    /**
     * 页面显示时，不可能把分的所有页都显示出来，只显示当前页码前面的几页
     *
     * @return
     */
    public int getFrom() {
        int from = current - 2;
        return from < 1 ? 1 : from;
    }

    /**
     * 页面显示时，不可能把分的所有页都显示出来，只显示当前页码后面的几页
     *
     * @return
     */
    public int getTo() {
        int to = current + 2;
        int total = getTotal();
        return to > total ? total : to;
    }
}
