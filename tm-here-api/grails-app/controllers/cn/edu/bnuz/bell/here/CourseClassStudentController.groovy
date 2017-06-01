package cn.edu.bnuz.bell.here

import cn.edu.bnuz.bell.http.BadRequestException
import cn.edu.bnuz.bell.http.ServiceExceptionHandler

class CourseClassStudentController implements ServiceExceptionHandler {
    CourseClassStudentService courseClassStudentService

    def show(String teacherId, String courseClassId, String id) {
        renderJson courseClassStudentService.show(teacherId, UUID.fromString(courseClassId), id)
    }

    def patch(String teacherId, String courseClassId, String id, String op) {
        Boolean result
        switch (op) {
            case 'DISQUALIFY':
                result = courseClassStudentService.disqualify(teacherId, UUID.fromString(courseClassId), id)
                break
            case 'QUALIFY':
                result = courseClassStudentService.qualify(teacherId, UUID.fromString(courseClassId), id)
                break
            default:
                throw new BadRequestException()
        }

        renderJson([result: result])
    }
}
