package cn.edu.bnuz.bell.here

import cn.edu.bnuz.bell.http.BadRequestException
import cn.edu.bnuz.bell.organization.Teacher
import cn.edu.bnuz.bell.workflow.Activities
import cn.edu.bnuz.bell.workflow.ListCommand
import cn.edu.bnuz.bell.workflow.ListType
import cn.edu.bnuz.bell.workflow.State
import cn.edu.bnuz.bell.workflow.commands.AcceptCommand
import cn.edu.bnuz.bell.workflow.commands.RejectCommand
import grails.transaction.Transactional

@Transactional
class FreeListenApprovalService extends FreeListenCheckService {
    def getCounts(String userId) {
        [
                (ListType.TODO): FreeListenForm.countByStatus(State.CHECKED),
                (ListType.DONE): FreeListenForm.countByApprover(Teacher.load(userId)),
                (ListType.TOBE): FreeListenForm.countByStatus(State.SUBMITTED),
        ]
    }

    def list(String userId, ListCommand cmd) {
        switch (cmd.type) {
            case ListType.TODO:
                return findTodoList(userId, cmd.args)
            case ListType.DONE:
                return findDoneList(userId, cmd.args)
            case ListType.TOBE:
                return findTobeList(userId, cmd.args)
            default:
                throw new BadRequestException()
        }
    }

    def findTodoList(String userId, Map args) {
        def forms = FreeListenForm.executeQuery '''
select new map(
  form.id as id,
  student.id as studentId,
  student.name as studentName,
  subject.name as subject,
  major.grade as grade,
  form.dateSubmitted as date,
  form.reason as reason,
  form.status as status
)
from FreeListenForm form
join form.student student
join student.major major
join major.subject subject
where form.status = :status
order by form.dateChecked
''',[status: State.CHECKED], args

        return [forms: forms, counts: getCounts(userId)]
    }

    def findDoneList(String userId, Map args) {
        def forms = FreeListenForm.executeQuery '''
select new map(
  form.id as id,
  student.id as studentId,
  student.name as studentName,
  subject.name as subject,
  major.grade as grade,
  form.dateSubmitted as date,
  form.reason as reason,
  form.status as status
)
from FreeListenForm form
join form.student student
join student.major major
join major.subject subject
where form.approver.id = :approverId
order by form.dateApproved desc
''',[approverId: userId], args

        return [forms: forms, counts: getCounts(userId)]
    }

    def findTobeList(String userId, Map args) {
        def forms = FreeListenForm.executeQuery '''
select new map(
  form.id as id,
  student.id as studentId,
  student.name as studentName,
  subject.name as subject,
  major.grade as grade,
  form.dateSubmitted as date,
  form.reason as reason,
  form.status as status
)
from FreeListenForm form
join form.student student
join student.major major
join major.subject subject
where form.status = :status
order by form.dateSubmitted
''',[status: State.SUBMITTED], args
        return [forms: forms, counts: getCounts(userId)]
    }

    void accept(AcceptCommand cmd, String userId, UUID workitemId) {
        FreeListenForm form = FreeListenForm.get(cmd.id)
        domainStateMachineHandler.accept(form, userId, Activities.APPROVE, cmd.comment, workitemId, cmd.to)
        form.approver = Teacher.load(userId)
        form.dateApproved = new Date()
        form.save()
    }

    void reject(RejectCommand cmd, String userId, UUID workitemId) {
        FreeListenForm form = FreeListenForm.get(cmd.id)
        domainStateMachineHandler.reject(form, userId, Activities.APPROVE, cmd.comment, workitemId)
        form.approver = Teacher.load(userId)
        form.dateApproved = new Date()
        form.save()
    }
}
