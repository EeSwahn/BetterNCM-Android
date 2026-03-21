package com.example.bna.data.network

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type

/**
 * 修复 Retrofit + Gson 出现的下面两个报错：
 *  1. "malformed JSON" - setLenient() 解决
 *  2. "JSON document was not fully consumed" - 自定义 Converter，读完后直接关流
 */
class LenientGsonConverterFactory private constructor(
    private val gson: Gson
) : Converter.Factory() {

    companion object {
        fun create(gson: Gson): LenientGsonConverterFactory = LenientGsonConverterFactory(gson)
    }

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *> {
        val adapter: TypeAdapter<*> = gson.getAdapter(com.google.gson.reflect.TypeToken.get(type))
        return LenientResponseBodyConverter(adapter)
    }

    // 使用 delegate 处理 request（requestBodyConverter 保持原来的 GsonConverterFactory）
    private val delegate = GsonConverterFactory.create(gson)

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit
    ) = delegate.requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit)
}

private class LenientResponseBodyConverter<T>(
    private val adapter: TypeAdapter<T>
) : Converter<ResponseBody, T> {
    override fun convert(value: ResponseBody): T? {
        val reader: JsonReader = JsonReader(value.charStream())
        reader.isLenient = true   // 兼容非标准 JSON
        return try {
            adapter.read(reader)  // 读取完整对象后直接返回，不检查是否还有剩余
        } finally {
            value.close()
        }
    }
}
