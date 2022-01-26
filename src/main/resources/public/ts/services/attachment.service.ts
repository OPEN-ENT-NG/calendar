import {ng} from 'entcore'

export interface AttachmentService {
    downloadAttachment(eventId : String, attachmentId : String, isUserAttachmentOwner : boolean);
}

export const attachmentService : AttachmentService = {
    downloadAttachment(eventId: String, attachmentId: String, isUserAttachmentOwner: boolean) {
        if (isUserAttachmentOwner) {
            window.open('/calendar/calendarevent/'+ eventId + '/attachment/' + attachmentId);
        } else {
            window.open(`/workspace/document/archive/${attachmentId}`, '_blank');
        }
    }

};

export const AttachmentService = ng.service('AttachmentService', (): AttachmentService => attachmentService);