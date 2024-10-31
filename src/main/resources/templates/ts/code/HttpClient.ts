/**
 * 用于发送请求的适配器
 */
export interface HttpClient {
    /**
     *
     * @param url 相对路径 如/api/login
     * @param method 请求方法 如 'get' 'post'
     * @param body 请求体
     * @param contentType 请求体类型
     */
    request<T>(url: string, method: string, body: any,contentType:string): Promise<T>
}
