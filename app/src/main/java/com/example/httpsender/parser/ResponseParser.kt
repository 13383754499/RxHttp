package com.example.httpsender.parser

import com.example.httpsender.entity.PageList
import com.example.httpsender.entity.Response
import rxhttp.wrapper.annotation.Parser
import rxhttp.wrapper.exception.ParseException
import rxhttp.wrapper.parse.TypeParser
import rxhttp.wrapper.utils.convertTo
import java.io.IOException
import java.lang.reflect.Type

/**
 * 输入T,输出T,并对code统一判断
 * User: ljx
 * Date: 2018/10/23
 * Time: 13:49
 *
 * 如果使用协程发送请求，wrappers属性可不设置，设置了也无效
 */
@Parser(name = "Response", wrappers = [PageList::class])
open class ResponseParser<T> : TypeParser<T> {
    /**
     * 此构造方法可适用任意Class对象，但更多用于带泛型的Class对象，如：List<List<Student>>>
     *
     * 如Java环境中调用
     * toObservable(new ResponseParser<List<List<Student>>>(){})
     * 等价于kotlin环境下的
     * toObservableResponse<List<List<Student>>>()
     *
     * 注：此构造方法一定要用protected关键字修饰，否则调用此构造方法将拿不到泛型类型
     */
    protected constructor() : super()

    /**
     * 该解析器会生成以下系列方法，前3个kotlin环境调用，后4个Java环境调用，所有方法内部均会调用本构造方法
     * toFlowResponse<T>()
     * toAwaitResponse<T>()
     * toObservableResponse<T>()
     * toObservableResponse(Type)
     * toObservableResponse(Class<T>)
     * toObservableResponseList(Class<T>)
     * toObservableResponsePageList(Class<T>)
     *
     * Flow/Await下 toXxxResponse<PageList<T>> 等同于 toObservableResponsePageList(Class<T>)
     */
    constructor(type: Type) : super(type)

    @Throws(IOException::class)
    override fun onParse(response: okhttp3.Response): T {
        val data: Response<T> = response.convertTo(Response::class, *types)
        var t = data.data //获取data字段
        if (t == null && types[0] === String::class.java) {
            /*
             * 考虑到有些时候服务端会返回：{"errorCode":0,"errorMsg":"关注成功"}  类似没有data的数据
             * 此时code正确，但是data字段为空，直接返回data的话，会报空指针错误，
             * 所以，判断泛型为String类型时，重新赋值，并确保赋值不为null
             */
            @Suppress("UNCHECKED_CAST")
            t = data.msg as T
        }
        if (data.code != 0 || t == null) { //code不等于0，说明数据不正确，抛出异常
            throw ParseException(data.code.toString(), data.msg, response)
        }
        return t
    }
}