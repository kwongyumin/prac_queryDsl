package com.practice.querydsl;


import com.practice.querydsl.model.Member;
import com.practice.querydsl.model.QMember;
import com.practice.querydsl.model.QTeam;
import com.practice.querydsl.model.Team;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.util.List;

import static com.practice.querydsl.model.QMember.member;
import static com.practice.querydsl.model.QTeam.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class QueryDslBasicTest {

    @PersistenceContext
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }
    @Test
    public void startJPQL() {
        //member1을 찾아라.
        String qlString =
                "select m from Member m " +
                        "where m.username = :username";
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }
    @Test
    public void startQuerydsl() {
        //컴파일 시점에서 오류를 잡아낼 수 있다.
        QMember m = member; // QMember member = QMember.member  //static 지정하여 member로 간단히 표현가능 (권장)
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1")) //파라미터 바인딩 처리
                .fetchOne();

        assert findMember != null;
        assertThat(findMember.getUsername()).isEqualTo("member1");

        /*
        동시성 문제  =  JPAQueryFactory 를 생성할 때 제공하는
        EntityManager(em) 에 달려있다.
        스프링은 여러 쓰레드에서 동시에 같은 EntityManager 에 접근해도 ,
        트랜젝션 마다 별도의 영속성 컨텍스트 를 제공하기 때문에 ,
        동시성 문제는 걱정하지 않아도 된다 .


         */

    }

    @Test // 검색
    public void search(){
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();


        assert findMember != null;
        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test // 검색
    public void searchAndParam(){
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.between(10,30))  //여러개를 ', ' 나누어도 and와 같음 , 동적쿼리 생성 시 용이함.
                .fetchOne();

        assert findMember != null;
        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void resultFetch(){
//        List<Member> fetch = queryFactory
//                .selectFrom(member)
//                .fetch(); // 리스트반환
//
//        Member fetchOne = queryFactory
//                .selectFrom(QMember.member)
//                .fetchOne(); // 단건 조회 (2건 이상이면 notUniqueException 발생 )
//
//        Member fetchFirst = queryFactory
//                .selectFrom(QMember.member)
//                .fetchFirst(); // 처음 한 건 조회
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults(); // QueryResults 페이징에서 사용
        results.getTotal();
        List<Member> content = results.getResults();


        long total = queryFactory  //전체 갯수 만 조회
                .selectFrom(member)
                .fetchCount();
    }

    /*
        회원 정렬 순서
        1. 회원 나이 내림차순(desc)
        2. 회원 이름 올림차순(asc)
        2 에서 회원 이름 없을 시 , 마지막에 출력
     */

    @Test
    public void sort(){
        em.persist(new Member(null,100));
        em.persist(new Member("member5",100));
        em.persist(new Member("member6",100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(
                        member.age.eq(100)
                )
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();

    }

    @Test // 페이징 처리
    public void paging1(){
        List<Member> results = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) // 시작 인덱스
                .limit(2) // 호출할 갯수
                .fetch();

        assertThat(results.size()).isEqualTo(2);

    }

    @Test
    public void paging2(){
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4); // 전체 갯수
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);


    }

    @Test
    public void aggregation(){
        List<Tuple> result = queryFactory
                .select( // 결과값이 튜플로 나온다.
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);


    }
    /*
        팀의 이름과 각 팀의 평균연령을 구하자.

     */

    @Test
    public void group() throws Exception{
        List<Tuple> results = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team) // 멤버의 팀과 팀을 조인
                .groupBy(team.name) //팀의 이름으로 그룹핑
                .fetch();

        Tuple teamA = results.get(0);
        Tuple teamB = results.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /*
        팀 A에 소속된 모든 회원
     */

    @Test
    public void join(){
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team) //innerJoin ,leftJoin, rightJoin
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1","member2");

    }

    /*
        세타 조인 - 연관관계가 없는 테이블간 조인
        회원의 이름이 팀 이름과 같은 회원 조회
     */

    @Test
    public void theta_join(){

        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory //모든 회원과 팀 의 테이블을 가져와 조인 시켜 값을 조회하는 방법 , DB가 성능 최적화를 한다.
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA","teamB");
    }

}
