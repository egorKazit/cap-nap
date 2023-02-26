// @ts-ignore
sap.ui.define(["sap/ui/core/mvc/Controller", "sap/m/MessageToast"], function (Controller, MessageToast) {
    "use strict";

    return {

        onAfterItemAdded: function (oEvent) {
            const item = oEvent.getParameter("item");
            const thread = item.getBindingContext().getObject();
            const data = {
                mediaType: item.getMediaType(),
                fileName: item.getFileName(),
                size: item.getFileObject().size,
                thread_ID: thread.ID,
                IsActiveEntity: thread.IsActiveEntity,
            };

            var settings = {
                url: "/odata/v4/ThreadService/Attachment", method: "POST", headers: {
                    "Content-type": "application/json",
                }, data: JSON.stringify(data),
            };

            // @ts-ignore
            return new Promise((resolve, reject) => {
                // @ts-ignore
                $.ajax(settings).done((results, textStatus, request) => {
                    resolve(results);
                }).fail((err) => {
                    reject(err);
                });
            }).then((attachment) => {

                const url = `/odata/v4/ThreadService/Attachment(ID=${attachment.ID},IsActiveEntity=${attachment.IsActiveEntity})/content`;
                item.setUploadUrl(url);
                const oUploadSet = item.getParent();
                oUploadSet.setHttpRequestMethod("PUT");
                oUploadSet.uploadItem(item);

            }).catch((err) => {
                const oUploadSet = item.getParent();
                oUploadSet.removeItem(item);
                alert("Can not load");
                console.log(err);
            });
        },

        onUploadCompleted: function (oEvent) {
            const oUploadSet = this.byId("uploadSet");
            oUploadSet.removeAllIncompleteItems();
            oUploadSet.getBinding("items").refresh();
        },

        onOpenPressed: function (oEvent) {
            oEvent.preventDefault();
            var item = oEvent.getSource();
            fetch(item.getUrl()).then((res) => {
                return res.blob();
            }).then((data) => {
                let hyperlink = document.createElement("a");
                hyperlink.href = window.URL.createObjectURL(data);
                hyperlink.download = item.getFileName();
                hyperlink.click();
            });
        },

        removePressed: function (oEvent) {
            oEvent.preventDefault();
            const item = oEvent.getParameter("item");
            const contentUrl = item.getUrl();
            const deleteRequest = {url: contentUrl.slice(0, contentUrl.lastIndexOf("/")), method: "DELETE"};
            const oUploadSet = this.byId("uploadSet");

            // @ts-ignore
            return new Promise((resolve, reject) => {
                // @ts-ignore
                $.ajax(deleteRequest).done((results, textStatus, request) => {
                    resolve(results);
                }).fail((err) => {
                    reject(err);
                });
            }).then((attachment) => {
                oUploadSet.removeItem(item);
            }).catch((err) => {
                console.log(err);
            });

        },
        
        formatThumbnailUrl: function (mediaType) {
            var iconUrl;
            switch (mediaType) {
                case "image/png":
                    iconUrl = "sap-icon://card";
                    break;
                case "text/plain":
                    iconUrl = "sap-icon://document-text";
                    break;
                case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":
                    iconUrl = "sap-icon://excel-attachment";
                    break;
                case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                    iconUrl = "sap-icon://doc-attachment";
                    break;
                case "application/pdf":
                    iconUrl = "sap-icon://pdf-attachment";
                    break;
                default:
                    iconUrl = "sap-icon://attachment";
            }
            return iconUrl;
        }


    };
});
