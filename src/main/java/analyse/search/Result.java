package analyse.search;

import java.util.Map;

import analyse.messageanalysis.Parameter;

public interface Result extends Comparable<Result> {
	/**
	 * getter
	 * @return Map<?,?> this.results
	 */
	public Map<?,?> getResults();
	
	/**
	 * getter
	 * @return String this.type
	 */
	public String getType();
	
	/**
	 * getter
	 * @return Double this.avg
	 */
	public Double getAvg();
	
	/**
	 * getter
	 * @return Double this.var
	 */
	public Double getVar();
	

	/**
	 * getter
	 * @return Long this.id
	 */
	public Long getId();
	
	/**
	 * get String representation of search informations
	 * @return String
	 */
	public String toString();
	
	/**
	 * get JSON representation of search informations
	 * @return String
	 */
	public String toJSON();
	
	/**
	 * getter
	 * @return Integer total
	 */
	public Integer getTotal();
	
	/**
	 * get search parameters
	 * @return
	 */
	public Parameter getParams();
}
