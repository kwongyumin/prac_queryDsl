package com.practice.querydsl.controller;

import com.practice.querydsl.model.Member;
import com.practice.querydsl.model.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Profile("local")
@Component
@RequiredArgsConstructor
public class InitMember {

    private final InitMemberService initMemberService;

    @PostConstruct // 스프링 라이프 싸이클로 인해 트랜잭셔널과 분리가 필요.
    public void init(){
        initMemberService.init();
    }

    @Component
    static class InitMemberService{
        @PersistenceContext
        private EntityManager entityManager;

        @Transactional
        public void init(){
            Team teamA = new Team("teamA");
            Team teamB = new Team("teamB");

            entityManager.persist(teamA);
            entityManager.persist(teamB);

            for(int i = 0; i < 100; i++){
                Team selectedTeam = i % 2 == 0 ? teamA : teamB;
                entityManager.persist(new Member("member" + i , i , selectedTeam));
            }

        }
    }
}
