package com.example.cae.user.interfaces.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public class UserPageQueryRequest {
    @Min(value = 1, message = "pageNum必须大于等于1")
    private Integer pageNum;
    @Min(value = 1, message = "pageSize必须大于等于1")
    @Max(value = 200, message = "pageSize不能超过200")
    private Integer pageSize;
    private String username;
    private String realName;
    private Integer status;
    @Positive(message = "roleId必须大于0")
    private Long roleId;

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
}
