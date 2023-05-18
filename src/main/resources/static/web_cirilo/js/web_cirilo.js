const webCirilo = (() => {

  console.log("Initializing cirilo-web library!");

  /**
   * Intended to be put on onsubmit attributes on html forms.
   * Appends input valiue to form action value.
   * @param {*} formId id of form that should be manipulated
   * @param {*} inputId id of input which's path should be appended
   * @returns true
   */
  const appendFormSubmitPath = (formId, inputId) => {
    let form = document.getElementById(formId);
    let input = document.getElementById(inputId);
    form.action = form.action + input.value;
    return true;
  };

  /**
   * Adds hidden input field containing md5 checksum of selected file, to form with given id. 
   * Needed to build a valid request containing a checksum as 'checksum' request variable.
   */
  const addChecksumRequestParam = (inputId, formId) => {
    // select file input element
    document.getElementById(inputId).addEventListener('change', async (evt) => {
      
      

      // access file content
      let inputElem = document.getElementById(inputId);

      if(inputElem.files.length > 1){
        alert("You are only allowed to upload one file at once! Reloading page now");
        window.location.reload();
      }

      let file = inputElem.files[0];
      let buffer = await file.arrayBuffer();  // array buffer works more consistently across different mimetypes

      // calculate hash value
      let hexHash = SparkMD5.ArrayBuffer.hash(buffer);        // hex hash

      // add hidden input to apply checksum request parameter with md5 checksum
      headerInput = document.createElement("input");
      headerInput.setAttribute("type", "hidden");
      headerInput.setAttribute("value", hexHash);
      headerInput.setAttribute("name", "md5Checksum");
      document.getElementById(formId).appendChild(headerInput);

    });

  } 


  /**
   * 
   * @param {*} formId 
   */
  const postIngestSubmit = (formId) => {

    // makes sure that post url is correct
    // more robust to use pathname --> excludes url parameters like ?page=3
    let url = window.location.pathname
        .replace("objects", "")
        .replace("ingest", "");

    // remove last trailing /
    url = url.substring(0, url.length -1);


    const form = document.getElementById(formId);

    form.addEventListener('submit', e => {
      e.preventDefault();
  
      // collecting selected files from input + zipping
      const files = document.querySelector('[type=file]').files;
      // file select validation
      if(!files || (files.length === 0)){
        let msg = "You have to select submission information packages for the ingest to work!";
        console.error(msg);
        return alert(msg);
      }
      if(files.length > 10000){
        let msg = "You cannot ingest more than 10000 files at the moment";
        console.error(msg);
        return alert(msg);
      }

      // construct Submission Information Packages
      const fileMap =  _constructSIPs(files);
      Object.keys(fileMap).forEach(foldername => {
        let folderFiles =  fileMap[foldername];
        if(folderFiles.length === 0) {
          return console.warn(`Empty SIP: Skipping SIP for object ${foldername} because it doesn't contain any files = datastreams.`);
        }

        const formData = new FormData();
        let zip = new JSZip();

        folderFiles.forEach(file => {
          zip.file(foldername + "/" + file.name, file);
        });

        // do the request based on folders
        zip.generateAsync({type: "blob"})
            .then(function (content) {
              // constructs a multipart request via formdata
              formData.append("subInfoPackZIP", content);

              let selects = document.querySelectorAll(`#${formId} select`)
              // corePrototype no multi select
              formData.append(selects[0].getAttribute("name"), selects[0].value);

              fetch(url, {
                method: 'POST',
                body: formData,
                redirect: "follow"
              }).then(response => {
                if(response.ok){
                  console.info("Successfully ingested object:", foldername);
                } else {
                  let baseErrMsg = "Failed to ingest object: " + foldername;
                  console.error(baseErrMsg);
                  response.json().then(jsonResponse => {
                    console.error(baseErrMsg, " Reason: " + jsonResponse.message);
                  }).catch(() => console.error("Failed to parse error response json"))
                }
              }).catch(err => {
                console.error(err);
                //alert("Failed to ingest given files. Check console for more detailed error reporting. Aborting operations...")
              });
            });

      });
    });
  }

  /**
   *
   * @param formId {string} id of form that should be used to change properties on
   * a digital object.
   */
  const applySubmitObjectMetaPropertiesListener = (formId) => {
    const url = window.location.href;
    const form = document.getElementById(formId);

    form.addEventListener('submit', e => {
      e.preventDefault();
      const formData = new FormData();

      // selects all inputs that can apply text.
      let inputs = document.querySelectorAll(`#${formId} input[type="text"]`)

      // add formdata
      inputs.forEach((input, index) => {
        if((index === inputs.length-1) || (index === inputs.length-2)){
          // skip last two inputs (they define the new property)
        } else {
          formData.append(input.getAttribute("name"), input.value);
        }
      });

      // last two inputs allow to set a new property
      let metaPropertyKey = inputs.item(inputs.length-2).value;
      let metaPropertyValue = inputs.item(inputs.length-1).value;
      if(metaPropertyKey && metaPropertyValue){
        formData.append(metaPropertyKey,metaPropertyValue );
      }

      fetch(url, {
        method: 'PUT',
        body: formData,
        redirect: "manual"
      }).then(response => {
        // fetch might return 0 (which is not okay but somehow a bug related to redirects)
        if(!response.ok && (response.status !== 0)){
          alert("Error applying new property to the digital object - check console for more details (most likely either the key or value are not valid turtle or URIs).");
          response.json().then(errJson => {
            console.error(errJson);
            console.error(errJson["message"]);
            console.error(errJson["trace"]);
            //document.body.textContent = errJson["trace"];
          })
        } else {
          window.location.reload();
        }

      }).catch(err => {
        console.error("Failed to PUT object with given formdata: ", formData);
        console.error(err);
        alert("Failed to apply new property values");
      });
    });
  }

  /**
   * Converts given fileList in to a map of Submission Information Packages, containing
   * the folder-name with the contained files.
   * Operates NOT on folder level (are not inside fileList) but on
   * @param files { FileList } List of files to be converted to map pof Submission Information Packages
   * @private
   * @return {Object.<string, File[]>} Map of Submission Information Packages.
   */
  const _constructSIPs = (files) => {

    //property is the submission information package "foldername".
    let fileMap = {};

    for (let i = 0; i < files.length; i++) {
      let file = files[i];
      //console.log("Got file with path ", file.webkitRelativePath, file);
      if(!file.webkitRelativePath.includes("/")) {
        let msg = `SubmissionPackageViolation - All files need to be contained in a folder of the related Submission Information Package - Happened at file: ${file.webkitRelativePath}`;
        console.error(msg, file)
        throw new TypeError(msg);
      }

      let splitArray = file.webkitRelativePath.split("/");

      // skipping files - only processing folder as digital objects
      if(file.webkitRelativePath.includes(".") && (splitArray.length === 2)){
        console.warn("SIPs can only be represented by folder and not files. Skipping file with path", file.webkitRelativePath);
        continue;
      }

      let containedFolderName = splitArray[1];
      // add an empty property if just the folder exists
      if(!fileMap[containedFolderName])fileMap[containedFolderName] = [];

      if(splitArray.length > 3){
        console.warn("Nested files in SIPs are not supported: Skipping nested file at path", file.webkitRelativePath);
        continue;
      }

      // add array of files to map
      if(fileMap[containedFolderName]){
        fileMap[containedFolderName].push(file);
      } else {
        fileMap[containedFolderName] = [file];
      }
    }

    console.info("*** Constructed filemap: ", fileMap);
    return  fileMap;
  }


  return {
    appendFormSubmitPath,
    addChecksumRequestParam,
    postIngestSubmit,
    applySubmitObjectMetaPropertiesListener
  };
})();
