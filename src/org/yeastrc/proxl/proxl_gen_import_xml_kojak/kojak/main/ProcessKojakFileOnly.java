package org.yeastrc.proxl.proxl_gen_import_xml_kojak.kojak.main;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.yeastrc.proxl.proxl_gen_import_xml_kojak.common.constants.SearchProgramNameKojakImporterConstants;
import org.yeastrc.proxl.proxl_gen_import_xml_kojak.common.kojak.IsAllProtein_1or2_Decoy;
import org.yeastrc.proxl.proxl_gen_import_xml_kojak.common.kojak.KojakFileReader;
import org.yeastrc.proxl.proxl_gen_import_xml_kojak.common.kojak.KojakPsmDataObject;
import org.yeastrc.proxl.proxl_gen_import_xml_kojak.kojak.objects.LinkTypeAndReportedPeptideString;
import org.yeastrc.proxl_import.api.xml_dto.ProxlInput;
import org.yeastrc.proxl_import.api.xml_dto.Psm;
import org.yeastrc.proxl_import.api.xml_dto.Psms;
import org.yeastrc.proxl_import.api.xml_dto.ReportedPeptide;
import org.yeastrc.proxl_import.api.xml_dto.ReportedPeptides;
import org.yeastrc.proxl_import.api.xml_dto.SearchProgram;
import org.yeastrc.proxl_import.api.xml_dto.SearchProgramInfo;
import org.yeastrc.proxl_import.api.xml_dto.SearchPrograms;

public class ProcessKojakFileOnly {

	private static final Logger log = Logger.getLogger( ProcessKojakFileOnly.class );
	
	// private constructor
	private ProcessKojakFileOnly() { }
	
	public static ProcessKojakFileOnly getInstance() {
		return new ProcessKojakFileOnly();
	}

	
	/**
	 * @param kojakOutputFile
	 * @param proxlInputRoot
	 * @param psmMatchingAndCollection
	 * @throws Exception 
	 */
	public void processKojakFile( 
			
			File kojakOutputFile,
			ProxlInput proxlInputRoot ) throws Exception {
		

		SearchProgramInfo searchProgramInfo = proxlInputRoot.getSearchProgramInfo();
		
		SearchPrograms searchPrograms = searchProgramInfo.getSearchPrograms();
		List<SearchProgram> searchProgramList = searchPrograms.getSearchProgram();
		
		SearchProgram searchProgram = new SearchProgram();
		searchProgramList.add( searchProgram );
		
		searchProgram.setName( SearchProgramNameKojakImporterConstants.KOJAK );
		searchProgram.setDisplayName( SearchProgramNameKojakImporterConstants.KOJAK );
		searchProgram.setDescription( null );
		
		
		KojakFileReader kojakFileReader = null;
		
		try {
			
			//  The reader reads the version line and the header lines in the getInstance(...) method
			
			kojakFileReader = KojakFileReader.getInstance( kojakOutputFile );
			
			PopulateOnlyKojakAnnotationTypesInSearchProgram.getInstance().populateKojakAnnotationTypesInSearchProgram( searchProgram, kojakFileReader );
			
			searchProgram.setVersion( kojakFileReader.getProgramVersion() );
			
			AddKojakOnlyAnnotationSortOrder.getInstance().addAnnotationSortOrder( searchProgramInfo );
			
			AddKojakOnlyDefaultVisibleAnnotations.getInstance().addDefaultVisibleAnnotations( searchProgramInfo );
			
			
			Map<String, ReportedPeptide> reportedPeptidesKeyedOnReportedPeptideString = new HashMap<>();
			
			//  Process the data lines:

			while (true) {

				KojakPsmDataObject kojakPsmDataObject;

				try {
					kojakPsmDataObject = kojakFileReader.getNextKojakLine();

				} catch ( Exception e ) {

					String msg = "Error reading Kojak file (file: " + kojakOutputFile.getAbsolutePath() + ") .";

					log.error( msg, e );

					throw e;
				}

				if ( kojakPsmDataObject == null ) {

					break;  //  EARLY EXIT from LOOOP
				}
				
				System.out.println( "Processing Kojak record for scan number: " + kojakPsmDataObject.getScanNumber() );

				if ( IsAllProtein_1or2_Decoy.getInstance().isAllProtein_1or2_Decoy( kojakPsmDataObject, proxlInputRoot) ) {
					
					System.out.println( "All proteins for Protein #1 or Protein #2 are decoys so skipping this Kojak record."
							+ "  scan number: " + kojakPsmDataObject.getScanNumber() );
					
					//   All proteins for Protein #1 or Protein #2 are decoys so skipping this Kojak record.
					
					continue;  //   EARLY CONTINUE to next record
				}
				
				LinkTypeAndReportedPeptideString linkTypeAndReportedPeptideString = 
						GetLinkTypeAndReportedPeptideString.getInstance().getLinkTypeAndReportedPeptideString( kojakPsmDataObject );
				
				if ( linkTypeAndReportedPeptideString == null ) {
					
					//  Kojak did not make an identification so this record is skipped.
					
					continue;  //  EARY CONTINUE
				}
				
				
				String reportedPeptideString = linkTypeAndReportedPeptideString.getReportedPeptideString();
				
				ReportedPeptide reportedPeptide = 
						reportedPeptidesKeyedOnReportedPeptideString.get( reportedPeptideString );
				
				if ( reportedPeptide == null ) {
					
					//  No ReportedPeptide object for this reportedPeptideString 
					//  so create it and store in the List and in the Map
					
					reportedPeptide = 
							PopulateProxlInputReportedPeptideFromKojakOnly.getInstance()
							.populateProxlInputReportedPeptide( kojakPsmDataObject, linkTypeAndReportedPeptideString );
					
					ReportedPeptides reportedPeptides = proxlInputRoot.getReportedPeptides();
					
					if ( reportedPeptides == null ) {
						
						reportedPeptides = new ReportedPeptides();
						proxlInputRoot.setReportedPeptides( reportedPeptides );
					}
					
					List<ReportedPeptide> reportedPeptideList = reportedPeptides.getReportedPeptide();
					
					reportedPeptideList.add( reportedPeptide );
					
					reportedPeptidesKeyedOnReportedPeptideString.put( reportedPeptideString, reportedPeptide );
				}
				
				Psms psms = reportedPeptide.getPsms();
				
				if ( psms == null ) {
					
					psms = new Psms();
					reportedPeptide.setPsms( psms );
				}
				
				List<Psm> psmList = psms.getPsm();
				
				
				Psm proxlInputPsm = 
						PopulateProxlInputPsmFromKojakOnly.getInstance().populateProxlInputPsm( kojakPsmDataObject );
				
				psmList.add( proxlInputPsm );
				
			}
			
		} catch ( Exception e ) {
			
			String msg = "Error processing Kojak file: " + kojakOutputFile.getAbsolutePath();
			log.error( msg );
			throw e;
		
		} finally {
			
			if ( kojakFileReader != null  ) {
				
				kojakFileReader.close();
			}
			
		}
		
		
		
	}
	

}