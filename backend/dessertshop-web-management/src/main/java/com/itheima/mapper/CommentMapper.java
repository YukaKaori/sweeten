package com.itheima.mapper;

import com.itheima.pojo.Comment;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CommentMapper {

    @Select("select * from comments where dessert_id = #{dessertId} order by created_at desc")
    List<Comment> selectByDessertId(@Param("dessertId") Integer dessertId);

    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("insert into comments(dessert_id, content, rating, user_id, username, avatar_bg, created_at) " +
            "values(#{dessertId}, #{content}, #{rating}, #{userId}, #{username}, #{avatarBg}, #{createdAt})")
    void insert(Comment comment);

    @Delete("delete from comments where id = #{id}")
    void deleteById(Integer id);

    @Select("select count(*) from comments where dessert_id = #{dessertId}")
    int countByDessertId(@Param("dessertId") Integer dessertId);

    @Select("select * from comments order by created_at desc limit #{limit}")
    List<Comment> selectLatest(@Param("limit") Integer limit);
}
