package com.mealorder.behavior.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mealorder.behavior.entity.UserBehavior;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserBehaviorMapper extends BaseMapper<UserBehavior> {

    /** 单条多 VALUES 批量写：埋点管道 flush 专用，SQL 日志可见一条 INSERT 多组值 */
    @Insert("<script>INSERT INTO user_behavior (id, user_id, item_id, action, scene, created_at) VALUES "
            + "<foreach collection='list' item='b' separator=','>"
            + "(#{b.id}, #{b.userId}, #{b.itemId}, #{b.action}, #{b.scene}, #{b.createdAt})"
            + "</foreach></script>")
    int insertBatch(@Param("list") List<UserBehavior> list);
}