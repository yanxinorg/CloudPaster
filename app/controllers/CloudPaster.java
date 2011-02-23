package controllers;

import java.util.List;

import models.Paster;
import models.Paster.QueryResult;
import models.User;
import notifiers.Notifier;

import org.apache.commons.lang.StringUtils;

import play.Play;
import play.cache.CacheFor;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.mvc.After;
import play.mvc.Before;
import play.mvc.Controller;

public class CloudPaster extends Controller {
	private static final long MINS_30 = 30*60*1000L;
	static final String KEY_USER = "user.key";
	@Before
	static void startTimer() {
	}
	@After
	static void after() {
		session.put(Auth.KEY_TIMESTAMP, System.currentTimeMillis());
	}
	
	@Before(unless={"login","googlelogin","yahoologin","index","view","search","intro"})
	static void checkAuthenticated() {
	    if(!session.contains(KEY_USER)) {
	        Auth.login();
	    }
	    if(session.contains("timestamp")) {
	    	long last = Long.parseLong(session.get(Auth.KEY_TIMESTAMP));
	    	if(System.currentTimeMillis() - last > MINS_30 ) {
	    		Auth.login();
	    	}
	    }
	}
	/**
	 * 最近活跃
	 */
	static public  void activity() {
		render();
	}
	/**
	 * 问题
	 */
	static public  void questions() {
		render();
	}
	/**
	 * 等待回答
	 */
	static public  void unanswered() {
		render();
	}
	/**
	 * 标签和分类
	 */
	static public void tags() {
		render();
	}
	/**
	 * 用户
	 */
	static public void users() {
		render();
	}
	/**
	 * 提问
	 */
	static public void ask() {
		render();
	}
	public static void index() {
		if(session.contains(KEY_USER)) {			
			render();
		} else {
			intro();
		}
	}
	//@CacheFor("15s")
	public static void load(int from) {
		List<Paster> pasters = Paster.findAll(from, 10);
		long count = Paster.count();
		request.format="json";
		render(pasters, count, from);
	}
	public static void loadmy(int from) {
		User user = getLoginUser();
		List<Paster> pasters = Paster.findByCreator(user.email, from, 10);
		long count = Paster.countByCreator(user.email);
		request.format="json";
		render("@load",pasters, count, from);
	}
	@CacheFor("15s")
	public static void loadMostUseful() {
		List<Paster> pasters = Paster.findMostUseful(0, 10);
		int from = 0;
		long count = 10;
		request.format="json";
		render("@load",pasters, count , from );
	}
	public static void intro() {
		render();
	}
	public static void prepaste() {
		render();
	}
	
	public static void my() {
		render();
	}

	public static void view(String key) {
		Paster paster = Paster.getByKey(key); 
		render(paster);
	}
	
	public static void search(String keywords,int from) {
		if(StringUtils.isNotEmpty(keywords)) {
			QueryResult search = Paster.search(keywords,from,10);
			long count = search.count;
			List<Paster> pasters = search.results;
			render(pasters,count,from,keywords);
		}else {
			flash.error("请输入要查询的内容。");
			render();
		}
	}
	
	public static void delete(String key) {
		User user = getLoginUser();
		Paster obj = Paster.getByKey(key);
		if (obj != null && obj.creator.equals(user.email)) {
			obj.remove();
		}
		my();
	}

	public static void paste(@Required(message="title required") String title,
			@Required(message = "content is required.") String content,
			String tagstext) {
		if (!Validation.hasErrors()) {
			User user = getLoginUser();
			Paster paster = Paster.createAndSave(title,content, user.email,tagstext);
			if(Boolean.valueOf(Play.configuration.getProperty("notifier.enabled","false"))) {
				Notifier.paste(user.email, paster);
			}
			success(paster);
		} else {
			fail("ERR-001", "内容字段不能为空");
		}
	}

	static void fail(String code,String message) {
		request.format="json";
		render("@fail",code,message);
	}
	static void success(Paster paster) {
		request.format="json";
		render("@success",paster);
	}
	public static void useful(String key) {
		User user = getLoginUser();
		Paster paster = Paster.getByKey(key);
		paster.useful();
		success(paster);
	}
	public static void useless(String key) {
		User user = getLoginUser();
		Paster paster = Paster.getByKey(key);
		paster.useless();
		success(paster);
	}
	public static void ratingup(String key) {
		User user = getLoginUser();
		Paster paster = Paster.getByKey(key);
		paster.rating += 1;
		paster.save();
		success(paster);
	}
	public static void ratingdown(String key) {
		User user = getLoginUser();
		Paster paster = Paster.getByKey(key);
		paster.rating -=1;
		paster.save();
		success(paster);
	}
	
	static User getLoginUser() {
		String userkey = session.get(KEY_USER);
		if(userkey == null)
			return null;
		User user = User.getByKey(userkey);
		return user;
	}
}
