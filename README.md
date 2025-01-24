# Bridge
Bridge用于为SpringMvc项目生成TypeScript客户端代码。并且为基于TS的前端项目提供类型提示。实现前后端自动对接。  

支持Java和Kotlin语言。  

对于后端开发，不再需要编写接口文档。  
对于前端开发，不再需要根据文档编写对接代码。
通过Bridge调用接口可以获得类似RPC一样调用本地方法的体验。  



# 开始

Maven 引入依赖
```xml

<dependency>
   <groupId>io.github.swqxdba</groupId>
   <artifactId>bridge</artifactId>
   <version>0.0.9</version>
</dependency>

```
在项目中 
新建一个Main方法然后运行

```java

public class ApiGenerator {

    public static void main(String[] args) {
        genCode();
    }

    private static void genCode() {
       
        //可以修改生成的Ts属性名 比如根据JSON框架的注解
        BridgeGlobalConfig.setPropertyNameConverter(new PropertyNameConverter() {
            @NotNull
            @Override
            public String convert(@NotNull String s, @NotNull Class<?> aClass) {
                try {
                    Field declaredField = aClass.getDeclaredField(s);
                    JsonProperty declaredAnnotation = declaredField.getDeclaredAnnotation(JsonProperty.class);
                    if(declaredAnnotation!=null){
                        return declaredAnnotation.value();
                    }
                    JSONField jsonField = declaredField.getDeclaredAnnotation(JSONField.class);
                    if(jsonField!=null){
                        return jsonField.name();
                    }
                    return s;
                } catch (NoSuchFieldException e) {
                   return s;
                }
            }
        });

        //输入你项目的源码的绝对文件路径 该路径用于生成代码注释。如果不设置，生成的TS代码中就没有注释了。
        BridgeGlobalConfig.setSourceCodeDir("C:\\Users\\yourname\\IdeaProjects\\yourprojectname\\src");
        //指定扫描一个类所在包下的所有controller，可以指定一个条件来进行过滤
        List<ControllerMeta> controllerMetas = BridgeUtil.scan(Application.class,
                name -> name.contains("Controller"));
        
        //可以忽略一些controller的方法参数
        List<Class<?>> ignoreParamParentTypes = new ArrayList<>();
        ignoreParamParentTypes.add(ServletRequest.class);
        ignoreParamParentTypes.add(ServletResponse.class);
        TsGenGlobalConfig.getIgnoreParamParentTypes().addAll(ignoreParamParentTypes);

        TsGenerator tsGenerator = new TsGenerator();
        //生成时覆盖旧的文件
        tsGenerator.setOverrideFile(true);

        //设置生成的Ts路径,请确保生成的路径存在 可能需要手动创建一下文件夹
        tsGenerator.setBasePath("C:\\Users\\yourname\\WebstormProjects\\yourprojectname\\src\\api");

        //生成时 会把上述文件夹清空后重新生成
        tsGenerator.setCleanBasePath(true);
        //执行Ts代码生成
        tsGenerator.generate(controllerMetas);

    }
}
```

### 更多配置
详见 `TsGenGlobalConfig`和`BridgeGlobalConfig`

# 前端接入
生成的文件中 有两个特殊的文件
Apis.ts 和 HttpClient.ts  

`Apis.ts`中包含了所有的接口，是一个API聚合类，你可以在某个地方创建一个Apis对象然后export出去。    

`HttpClient.ts`定义了一个接口，用于实现发送请求，你需要自己实现这个接口。你可以使用Axios或者Fetch。  

`HttpClient.ts`

```ts

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


```
这里提供一个参考实现  
ApiUtil.ts
```ts
import axios, {AxiosRequestConfig} from "axios";
import {token} from "@/stores";
import {HttpClient} from "@/api/api/HttpClient";
import {Apis} from "@/api/api/Apis";
import {Response} from "@/api/types/Response";
import {ElMessage} from "element-plus";

const client = axios.create()
//你可以在这里带上你的cookie header等发送你的鉴权信息
client.interceptors.request.use((context) => {
    if (context.headers) {
        context.headers['x-access-token'] = token.value || ''
    }
    return context
})

/**
 * 需要实现一个httpClient,可以使用axios fetch等都行。可以在这里做统一处理或者过滤、拦截
 */
class HttpClientImpl implements HttpClient {

    request<T>(url: string, method: string, body: any, contentType: string): Promise<T> {
        //这里可以从获取项目的基础路径 
        url = 'YOUR_BASE_URL'+url
        let config: AxiosRequestConfig = {
            url,
            method
        }
        config.headers = {
            'Content-Type': contentType
        }
        if (contentType === 'application/json') {
            for (let key in body) {
                if (body[key] === undefined) {
                    body[key] = null
                }
            }
            config.data = JSON.stringify(body)
        } else if (contentType === 'application/x-www-form-urlencoded') {
            let urlSearchParams = this.toUrlSearchParams(body);
            if (method === 'get' || method === 'GET') {
                config.url += '?' + urlSearchParams.toString()
            } else {
                config.data = urlSearchParams
            }
        } else if (contentType === 'multipart/form-data') {
            config.data = this.toFormData(body)
        }
        //对于返回的响应进行处理
        return client.request(config).then(response => {
            if (response.status !== 200) {
                throw new Error('请求失败,请稍后重试')
            }
            const resp = response.data
            if (resp.code === '0000') {
                return resp as T
            } else {
                ElMessage.error(resp.message)
                console.log('resp', resp)
                throw new Error(resp.message || '')
            }
        })

    }

    private toUrlSearchParams(body: any): URLSearchParams {
        let param: URLSearchParams
        if (body instanceof URLSearchParams) {
            param = body
        } else {
            param = new URLSearchParams()
            for (let key in body) {
                const bodyElement = body[key];
                if (Array.isArray(bodyElement)) {
                    (bodyElement as Array<any>).forEach(item => {
                        param.append(key, item)
                    })
                } else {
                    param.append(key, bodyElement)
                }
            }
        }
        return param
    }

    private toFormData(body: any): FormData {
        let formData: FormData
        if (body instanceof FormData) {
            formData = body
        } else {
            formData = new FormData()
            for (let key in body) {
                const value = body[key];
                if (value instanceof Array) {
                    value.forEach((item: any) => {
                        formData.append(key, item)

                    })
                }
                if (value instanceof FileList) {
                    for (let i = 0; i < value.length; i++) {
                        formData.append(key, value.item(i) as any)
                    }
                } else {
                    formData.append(key, value)

                }

            }

        }
        return formData
    }
}

/**
 * 使用httpClient实现构造聚合Api对象 该对象中有所有的接口
 */
//实际使用时，将这个对象导出到项目中使用
export const apis = new Apis(new HttpClientImpl());
```
在上述文件中，我们导出了一个apis全局变量。 那么在其他前端代码中，我们可以直接引入使用。

```ts

import {apis} from "./ApiUtil";
const user = await apis.UserApi.findUser(userId)
```

# 注释搬运
Bridge会在生成TS代码时，从你的java或者kotlin代码中搬运代码注释    
包括Controller类上的注释，Controller方法上的注释    
其他Dto、Vo类型中类上的注释和字段的注释。  
暂不支持搬运继承自父级的注释。   

支持文档注释和行内注释  
```java
private String name;//用户名

/**
 * 用户名
 */
private String name;

```

支持读取getter方法上的注释 getName方法上的注释会被放到name属性中
```java

//用户名
public String getName(){
    
}

```

除了自动搬运，你还可以通过@ApiComment注解来指定注释。  

（注意，注释解析是通过正则实现的，并不严谨，一些情况下可能会匹配到错误的注释或者匹配不到注释，
目前已知在碰到有跨越多行的注解(Annotation,不是注释)时会找不到注释。  
这种情况下可以使用@ApiComment来覆盖，或者保证注解只占一行）

```java

@ApiComment("读取用户")
@PostMapping(value = "get", 
        produces = MediaType.APPLICATION_JSON_VALUE)
public User getUser(){}
```

```java

/**
 * 也可以改成这样 让注解只占一行
 */
@PostMapping(value = "get", produces = MediaType.APPLICATION_JSON_VALUE)
public User getUser(){}
```
# @ApiIgnore
如果你想要某个API被生成时排除，可以在Controller类上加上@ApiIgnore注解来排除整个Controller，  
或者在Controller的某个方法上加上@ApiIgnore注解进行单独的排除。  

# 实现细节
Bridge通过运行时反射来扫描读取Controller，然后通过对源码进行正则匹配来寻找方法、属性上的注释。  
这意味着可能会触发一些类加载逻辑，或者执行静态代码块中的代码，需要注意。  

# JavaBean
Bridge会为接口参数、返回值生成对应的TS类型定义，  
这个过程中是通过JavaBean规范来读取Dto、Vo上的属性。而不是通过Field读取的。  
如BeanUtils.getProperties()

# 使用建议
建议由一个开发人员在开发环境下运行代码生成，直接生成到前端项目路径中，然后通过git提交到前端代码库中  
