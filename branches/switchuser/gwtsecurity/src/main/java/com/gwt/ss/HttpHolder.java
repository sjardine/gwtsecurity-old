package com.gwt.ss;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.aspectj.lang.JoinPoint;

public class HttpHolder {

    HttpServletRequest request;
    HttpServletResponse response;

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public void setResponse(HttpServletResponse response) {
        this.response = response;
    }

    public boolean isGwt() {
        return GwtResponseUtil.isGwt(request);
    }

    public static HttpHolder getInstance(JoinPoint jp) {
        if (jp == null) {
            return null;
        } else {
            HttpHolder holder = new HttpHolder();
            for (Object obj : jp.getArgs()) {
                if (obj != null) {
                    if (obj instanceof HttpServletRequest) {
                        holder.setRequest((HttpServletRequest) obj);
                    } else if (obj instanceof HttpServletResponse) {
                        holder.setResponse((HttpServletResponse) obj);
                    }
                    if (holder.getRequest() != null && holder.getResponse() != null) {
                        break;
                    }
                }
            }
            return holder;
        }
    }
}