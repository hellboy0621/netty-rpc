package com.xtransformers.rpc.server;

import com.alibaba.fastjson.JSONObject;
import com.xtransformers.rpc.Mediator;
import com.xtransformers.rpc.RequestFuture;
import com.xtransformers.rpc.Response;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @author daniel
 * @date 2021-06-03
 */
@ChannelHandler.Sharable
public class ServerHandler extends ChannelInboundHandlerAdapter {

    /**
     * 读取客户端发送的数据
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        RequestFuture request = JSONObject.parseObject(msg.toString(), RequestFuture.class);
        Response response = Mediator.process(request);
        ctx.channel().writeAndFlush(JSONObject.toJSONString(response));
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {

    }
}
