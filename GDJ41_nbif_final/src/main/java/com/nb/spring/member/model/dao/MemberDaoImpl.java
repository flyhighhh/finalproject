package com.nb.spring.member.model.dao;

import java.util.Map;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import com.nb.spring.member.model.vo.Member;

@Repository
public class MemberDaoImpl implements MemberDao {
	
	@Override
	public Member loginMember(SqlSessionTemplate session,Map<String, String> param) {
		return session.selectOne("member.loginMember", param);
	}

	@Override
	public Member selectMemberNickName(SqlSessionTemplate session, String nickName) {
		
		return session.selectOne("member.selectMemberNickName", nickName);
	}

	@Override
	public int insertMember(SqlSessionTemplate session, Member m) {
		
		return session.insert("member.insertMember", m);
	}

}
