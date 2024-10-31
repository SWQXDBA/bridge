
export class ApiUtil {



   public static toForm(obj: any): FormData {
        const formData = new FormData();

        if (typeof obj !== 'object') {
            throw new Error('Invalid input. Object expected.');
        }

        for (const key in obj) {
            if (obj.hasOwnProperty(key)) {
                formData.append(key, obj[key]);
            }
        }

        return formData;
    }


    public static toJson(obj: any): string {
        if (typeof obj !== 'object') {
            throw new Error('Invalid input. Object expected.');
        }

        return JSON.stringify(obj);
    }

}
