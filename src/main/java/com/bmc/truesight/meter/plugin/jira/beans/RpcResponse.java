package com.bmc.truesight.meter.plugin.jira.beans;

/**
 *
 * @author Santosh Patil
 */
public class RpcResponse {

    private String jsonrpc;
    private int id;
    private RpcResult result;
    private String error;

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public RpcResult getResult() {
        return result;
    }

    public void setResult(RpcResult result) {
        this.result = result;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
