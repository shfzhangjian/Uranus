package com.soonsoft.uranus.web.mvc.model;

/**
 * IResultData
 */
public interface IResultData {

    IResultData setSuccess(boolean success);

    IResultData setTotalRows(int total);

    IResultData setMessage(String message);

    IResultData setData(Object data);

    IResultData setValue(String name, Object value);

}