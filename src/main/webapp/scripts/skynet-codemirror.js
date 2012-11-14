var codeMirrors = new Array();

function initializeCodeMirror(id, readOnly)
{
	var xmlTextArea = document.getElementById(id);
	myCodeMirror = CodeMirror.fromTextArea(
		xmlTextArea, 
		{
			mode: {name: "xmlpure"}, 
			lineNumbers: true,
			readOnly: readOnly,
			lineWrapping: true
		}
	);
	codeMirrors[id] = myCodeMirror;
}

function saveCodeMirror(id)
{
	if (codeMirrors[id] != null)
	{
		codeMirrors[id].save();
	}
}

function refreshCodeMirror(id)
{
	if (codeMirrors[id] != null)
	{
		// Workaround to https://issues.jboss.org/browse/RF-3986
		setTimeout(function() {codeMirrors[id].refresh();}, 0);
	}
}