package edu.rice.cs.hpc.traceviewer.data.abstraction;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.AbstractSourceProvider;

public class ProcessDataService extends AbstractSourceProvider {

	final static public String PROCESS_DATA_PROVIDER = "edu.rice.cs.hpc.traceviewer.services.ProcessDataService.data";
	private AbstractProcessData []traces;


	@Override
	public void dispose() {	}

	@Override
	public Map getCurrentState() {
		Map<String, Object> map = new HashMap<String, Object>(1);
		map.put(PROCESS_DATA_PROVIDER, traces);
		
		return map;
	}

	@Override
	public String[] getProvidedSourceNames() {
		return new String[] {PROCESS_DATA_PROVIDER};
	}

	public void setProcessData(AbstractProcessData[] traces) {
		this.traces = traces;
	}
	
	
	public boolean setProcessData(int index, AbstractProcessData trace) {
		boolean result = (traces != null && traces.length > index);
		if (result)
			traces[index] = trace;
		/*else
			System.err.println("PTS incorrect index: " + index + " out of " + (traces == null ? 0 : traces.length));*/
		return result;
	}
 	
	
	public AbstractProcessData getProcessData(int proc) {
		if (traces == null)
			return null;
		
		return traces[proc];
	}
	
	public int getNumProcessData() {
		if (traces == null)
			return 0;
		return traces.length;
	}
	
	public boolean isFilled() {
		if (traces != null) {
			for (AbstractProcessData trace: traces) {
				if (trace == null)
					return false;
			}
			return true;
		}
		return false;
	}
}
