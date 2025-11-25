package com.yunting.mapper;

import com.yunting.model.Task;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TaskMapper {

    int insert(Task task);

    Task selectById(@Param("taskId") Long taskId);

    List<Task> selectList(@Param("status") Integer status,
                          @Param("offset") Integer offset,
                          @Param("pageSize") Integer pageSize);

    long countByStatus(@Param("status") Integer status);
}

