package biweekly.property.marshaller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import biweekly.parameter.ICalParameters;
import biweekly.parameter.Value;
import biweekly.property.RecurrenceDates;
import biweekly.util.Duration;
import biweekly.util.Period;
import biweekly.util.StringUtils;
import biweekly.util.StringUtils.JoinCallback;

/*
 Copyright (c) 2013, Michael Angstadt
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met: 

 1. Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer. 
 2. Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution. 

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * Marshals {@link RecurrenceDates} properties.
 * @author Michael Angstadt
 */
public class RecurrenceDatesMarshaller extends ICalPropertyMarshaller<RecurrenceDates> {
	public RecurrenceDatesMarshaller() {
		super(RecurrenceDates.class, "RDATE");
	}

	@Override
	protected void _prepareParameters(RecurrenceDates property, ICalParameters copy) {
		Value value = null;
		if (property.getDates() != null) {
			if (!property.hasTime()) {
				value = Value.DATE;
			}
		} else if (property.getPeriods() != null) {
			value = Value.PERIOD;
		}
		copy.setValue(value);
	}

	@Override
	protected String _writeText(final RecurrenceDates property) {
		if (property.getDates() != null) {
			return StringUtils.join(property.getDates(), ",", new JoinCallback<Date>() {
				public void handle(StringBuilder sb, Date date) {
					String value = date(date).time(property.hasTime()).write();
					sb.append(value);
				}
			});
		} else if (property.getPeriods() != null) {
			return StringUtils.join(property.getPeriods(), ",", new JoinCallback<Period>() {
				public void handle(StringBuilder sb, Period period) {
					if (period.getStartDate() != null) {
						String value = date(period.getStartDate()).write();
						sb.append(value);
					}

					sb.append('/');

					if (period.getEndDate() != null) {
						String value = date(period.getEndDate()).write();
						sb.append(value);
					} else if (period.getDuration() != null) {
						sb.append(period.getDuration());
					}
				}
			});
		}
		return "";
	}

	@Override
	protected RecurrenceDates _parseText(String value, ICalParameters parameters, List<String> warnings) {
		String split[] = parseList(value);

		Value valueParam = parameters.getValue();
		if (valueParam == Value.PERIOD) {
			//parse as periods
			List<Period> periods = new ArrayList<Period>(split.length);
			for (String timePeriodStr : split) {
				String timePeriodStrSplit[] = timePeriodStr.split("/");

				if (timePeriodStrSplit.length < 2) {
					warnings.add("No end date or duration found, skipping time period: " + timePeriodStr);
					continue;
				}

				String startStr = timePeriodStrSplit[0];
				Date start;
				try {
					start = date(startStr).tzid(parameters.getTimezoneId(), warnings).parse();
				} catch (IllegalArgumentException e) {
					warnings.add("Could not parse start date, skipping time period: " + timePeriodStr);
					continue;
				}

				String endStr = timePeriodStrSplit[1];
				try {
					Date end = date(endStr).tzid(parameters.getTimezoneId(), warnings).parse();
					periods.add(new Period(start, end));
				} catch (IllegalArgumentException e) {
					//must be a duration
					try {
						Duration duration = Duration.parse(endStr);
						periods.add(new Period(start, duration));
					} catch (IllegalArgumentException e2) {
						warnings.add("Could not parse end date or duration value, skipping time period: " + timePeriodStr);
						continue;
					}
				}
			}
			return new RecurrenceDates(periods);
		} else {
			//parse as dates
			boolean hasTime = (valueParam == null || valueParam == Value.DATE_TIME);
			List<Date> dates = new ArrayList<Date>(split.length);
			for (String s : split) {
				try {
					Date date = date(s).tzid(parameters.getTimezoneId(), warnings).parse();
					dates.add(date);
				} catch (IllegalArgumentException e) {
					warnings.add("Skipping unparsable date: " + s);
				}
			}
			return new RecurrenceDates(dates, hasTime);
		}
	}
}
