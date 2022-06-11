package com.practice.querydsl;

import com.practice.querydsl.model.Hello;
import com.practice.querydsl.model.QHello;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@SpringBootTest
@Transactional
class PracQueryApplicationTests {

    //@Autowired // 스프링 현 버전에서는 이렇게 사용.
    @PersistenceContext //  자바 표준 스펙에서는 이렇게 사용해야한다.
    EntityManager em;


    @Test
    void contextLoads() {
        Hello hello = new Hello();
        em.persist(hello);

        JPAQueryFactory query = new JPAQueryFactory(em);
        QHello qHello = new QHello("h");

        Hello result  = query
                .selectFrom(qHello)
                .fetchOne();


        Assertions.assertThat(result).isEqualTo(hello);
//        Assertions.assertThat(result.getId()).isEqualTo(hello.getId());


    }

}
