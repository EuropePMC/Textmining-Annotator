package uk.ac.ebi.literature.textminingapi.parser;

import monq.jfa.Xml;
import org.apache.commons.collections.MapUtils;

import java.util.Map;

public class MwtParser {

	public MwtAtts parse(Map<String, String> map) {
		MwtAtts mwtAtts = new MwtAtts();
		if (MapUtils.isNotEmpty(map)) {
			String tagName = map.get(Xml.TAGNAME);
			String content = map.get(Xml.CONTENT);
			String db = map.get("db");
			String valMethod = map.get("valmethod");
			String domain = map.get("domain");
			String ctx = map.get("context");
			Integer wsize = Integer.parseInt((null == map.get("wsize") || map.get("wsize").equals("")) ? "0" : map.get("wsize"));
			String sec = map.getOrDefault("sec", "");
			mwtAtts = new MwtAtts(tagName, content, db, valMethod, domain, ctx, wsize, sec);
		}
		return mwtAtts;
	}
	
	
	public static class MwtAtts{
		String tagName;
		String content;
		String db;
		String valMethod;
		String domain;
		String ctx;
		Integer wsize;
		String sec;
		
		public MwtAtts() {
		}
		
		public MwtAtts(String tagName, String content, String db, String valMethod, String domain, String ctx,
				Integer wsize, String sec) {
			super();
			this.tagName = tagName;
			this.content = content;
			this.db = db;
			this.valMethod = valMethod;
			this.domain = domain;
			this.ctx = ctx;
			this.wsize = wsize;
			this.sec = sec;
		}
		public String getTagName() {
			return tagName;
		}
		public void setTagName(String tagName) {
			this.tagName = tagName;
		}
		public String getContent() {
			return content;
		}
		public void setContent(String content) {
			this.content = content;
		}
		public String getDb() {
			return db;
		}
		public void setDb(String db) {
			this.db = db;
		}
		public String getValMethod() {
			return valMethod;
		}
		public void setValMethod(String valMethod) {
			this.valMethod = valMethod;
		}
		public String getDomain() {
			return domain;
		}
		public void setDomain(String domain) {
			this.domain = domain;
		}
		public String getCtx() {
			return ctx;
		}
		public void setCtx(String ctx) {
			this.ctx = ctx;
		}
		public Integer getWsize() {
			return wsize;
		}
		public void setWsize(Integer wsize) {
			this.wsize = wsize;
		}
		public String getSec() {
			return sec;
		}
		public void setSec(String sec) {
			this.sec = sec;
		}
		
	}
}
