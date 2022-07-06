package com.practice.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MemberDto {

    private String username;
    private int age;

    @QueryProjection // DTO를 QFile로 생성 // QueryDsl에 대한 의존성을 가진다 .
    public MemberDto(String username,int age){
        this.username = username;
        this.age = age;
    }
}
