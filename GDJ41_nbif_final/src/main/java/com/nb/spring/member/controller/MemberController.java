package com.nb.spring.member.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;

import com.nb.spring.member.model.service.MemberService;
import com.nb.spring.member.model.service.SendEmailService;
import com.nb.spring.member.model.vo.Member;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/member")
@SessionAttributes({"loginMember"})
public class MemberController {
	
	
	
	@Autowired
	private MemberService service;      
	
	@Autowired
	private SendEmailService mailService;
	
	@Autowired
	private PasswordEncoder encoder;

	@PostMapping("/loginMember")
	public ModelAndView loginMember(ModelAndView mv, String email, String password) {
		Map<String, String> param = new HashMap<String, String>();
		param.put("email", email);
		//param.put("password", password);
		Member m = service.loginMember(param);
		if(m!=null&&encoder.matches(password, m.getPassword())) {
		
			mv.addObject("loginMember", m);
			mv.addObject("msg","로그인 성공");
			mv.addObject("loc","/");
		}else {
			mv.addObject("msg","로그인 실패, 다시 시도하세요.");
			mv.addObject("loc","/member/login");
		}
		mv.setViewName("common/msg");
		return mv;
	}
	
	@RequestMapping("/logout")
	public ModelAndView logout(HttpSession session, SessionStatus stauts, ModelAndView mv) {
		if(!stauts.isComplete()) {
			stauts.setComplete();
		}
		session.invalidate();
		String msg = "로그아웃 완료";
		String loc = "/";
		mv.addObject("msg", msg);
		mv.addObject("loc", loc);
		mv.setViewName("common/msg");
		return mv;
	}
	
	@RequestMapping("/myPage")
	public String myPage() {
		return "login/myPage";
	}
	
	@RequestMapping("/enrollMember")
	public String enrollmemberView() {
		return "login/enrollEmail";
	}
	
	@PostMapping("/email")
	@ResponseBody
	public Map sendEmail(HttpSession session, String userEmail) {
		log.debug(userEmail);
		String result="";
		String code="";
		session.removeAttribute("userEmail");
		try {
			code = mailService.mailSend(userEmail);			
		}catch(Exception e) {
			e.printStackTrace();
			throw new IllegalArgumentException();
		}
			
		if(code!=null&&code.length()>0) {
			result ="전송완료";
			session.setAttribute("emailCode", code);
			session.setAttribute("userEmail", userEmail);
		}else {
			result ="전송실패";
		}
		
		return Map.of("result",result);
	}
	
	@PostMapping("/certification")
	@ResponseBody
	public Map certification(String inputCode, HttpSession session) {
		
		String codeInSession = (String)session.getAttribute("emailCode");
		boolean result =false;
		if(codeInSession.equals(inputCode)) {
			// 같은 코드 
			result =true;
			session.removeAttribute("emailCode");
		}else {
			//실패 
			result =false;
		}
		
		
		return Map.of("result",result);
	}
	
	@RequestMapping("/enrollMemberMainView")
	public String enrollMemberMainView() {
		return "login/enrollMember";
	}
	
	@RequestMapping("/duplicationCheck")
	@ResponseBody
	public Map duplicationCheck(String nickName) {
		
		Member m  = service.selectMemberNickName(nickName);
		boolean result = false;
		if(m!=null) {
			result = false;
		}else {
			result =true;
		}
		return Map.of("result",result);
	}
	
	@PostMapping("/enrollMemberMain")
	public ModelAndView enrollMemberMain(@RequestParam Map<String, String> param,HttpSession session, ModelAndView mv) {
		log.debug("{}",param);
		String totalAddress = param.get("address")+" "+ param.get("detailAddress")+" "+param.get("plusAddress");
		String email = (String)session.getAttribute("userEmail");
		
		String encodingPw = encoder.encode(param.get("password"));
		log.debug(encodingPw);
		Member m = Member.builder()
				.memberName(param.get("name"))
				.password(encodingPw)
				.phone(param.get("phone"))
				.email(email)
				.nickName(param.get("nickName"))
				.address(totalAddress)
				.deliveryAddress(totalAddress)
				.build();
		
		int result = service.insertMember(m);
		
		if(result > 0) {
			
			mv.addObject("msg","회원가입 성공");
			mv.addObject("loc","/");
		}else {
			mv.addObject("msg","회원가입 실패. 다시 시도해주세요.");
			mv.addObject("loc","/member/enrollMember");
		}
		mv.setViewName("common/msg");
		return mv;
	
	}
	
	@RequestMapping("/findId")
	public String findId() {
		return "login/findId";
	}
	
	@PostMapping("/findIdEnd")
	public ModelAndView findIdEnd(String name, String phone, ModelAndView mv) {
		log.debug(name,phone);
		Member m = service.selectMemberNamePhone(Map.of("name",name,"phone",phone));
		log.debug("{}",m);
		
		if(m==null) {
			String msg = "없는 회원입니다.";
			String loc = "/login";
			mv.addObject("msg", msg);
			mv.addObject("loc", loc);
			mv.setViewName("common/msg");
			return mv;
		}
		
		
		
		
		String email = m.getEmail();
		String id = email.substring(0, email.indexOf("@"));
		String address = email.substring(email.indexOf("@"));
		String idFront = id.substring(0,id.length()-3);
		String idEnd = id.substring(id.length()-3);
		String temp="";
		
		for(int i=0; i<idEnd.length();i++) {
			temp+="*";
		}
		
		String modifyEmail = idFront+temp+address;
		
		mv.addObject("userId", modifyEmail);
		mv.addObject("userName",m.getMemberName());
		mv.setViewName("login/findIdConfirm");
		
		
		return mv;
	}
	
	@RequestMapping("/findPassword")
	public String findPassword() {
		return "login/findPassword";
	}
	
	@PostMapping("/findPasswordEnd")
	public ModelAndView findPasswordEnd(String phone, String email, ModelAndView mv) throws Exception {
		
		Map<String, String > param = Map.of("phone",phone,"email",email);
		Member m = service.selectMemberPhoneEmail(param);
		String msg = "";
		String loc = "";
		if(m==null) {
			msg = "없는 회원입니다.";
			loc = "/member/findPassword";
		}else {
			
			String newEncodingPw = encoder.encode(mailService.mailSendNewPassword(m.getEmail()));
			log.debug(newEncodingPw);
			Map<String, String> param2 = Map.of("memberNo",m.getMemberNo(),"newPw",newEncodingPw);
			int result = service.updatePassword(param2);
			
			log.debug("{}",result);
			if(result>0) {
				msg = "임시 비밀번호 발급완료";
				loc = "/member/login";
			}else {
				msg = "임시 비빌번호 발급 실패";
				loc = "/member/findPassword";
			}
			
		}
		mv.addObject("msg", msg);
		mv.addObject("loc", loc);
		mv.setViewName("common/msg");
		return mv;
	}
	
	@RequestMapping("/login")
	public String loginView() {
		return "login/loginView";
	}
	
	
	
	
	
	
}
