package com.example.cae.common.response;

import java.util.List;

public class PageResult<T> {
    private long total;
    private long pageNum;
    private long pageSize;
    private List<T> records;

    public static <T> PageResult<T> of(long total, long pageNum, long pageSize, List<T> records) {
        PageResult<T> result = new PageResult<>();
        result.total = total;
        result.pageNum = pageNum;
        result.pageSize = pageSize;
        result.records = records;
        return result;
    }

    public long getTotal() {
        return total;
    }

    public long getPageNum() {
        return pageNum;
    }

    public long getPageSize() {
        return pageSize;
    }

    public List<T> getRecords() {
        return records;
    }
}
